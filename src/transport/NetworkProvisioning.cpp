/*
 *
 *    Copyright (c) 2020 Project CHIP Authors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

#include <core/CHIPEncoding.h>
#include <core/CHIPSafeCasts.h>
#include <platform/internal/DeviceNetworkInfo.h>
#include <protocols/CHIPProtocols.h>
#include <support/CodeUtils.h>
#include <support/ErrorStr.h>
#include <support/SafeInt.h>
#include <transport/NetworkProvisioning.h>

#if CONFIG_DEVICE_LAYER
#include <platform/CHIPDeviceLayer.h>
#endif

namespace chip {

void NetworkProvisioning::Init(NetworkProvisioningDelegate * delegate, DeviceNetworkProvisioningDelegate * deviceDelegate)
{
    if (mDelegate != nullptr)
    {
        mDelegate->Release();
    }

    if (delegate != nullptr)
    {
        mDelegate = delegate->Retain();
    }

    if (mDeviceDelegate != nullptr)
    {
        mDeviceDelegate->Release();
    }

    if (deviceDelegate != nullptr)
    {
        mDeviceDelegate = deviceDelegate->Retain();
    }
}

NetworkProvisioning::~NetworkProvisioning()
{
    if (mDeviceDelegate != nullptr)
    {
        mDeviceDelegate->Release();

#if CONFIG_DEVICE_LAYER
        DeviceLayer::PlatformMgr().RemoveEventHandler(ConnectivityHandler, reinterpret_cast<intptr_t>(this));
#endif
    }

    if (mDelegate != nullptr)
    {
        mDelegate->Release();
    }
}

CHIP_ERROR NetworkProvisioning::HandleNetworkProvisioningMessage(uint8_t msgType, System::PacketBuffer * msgBuf)
{
    CHIP_ERROR err = CHIP_NO_ERROR;

    switch (msgType)
    {
    case NetworkProvisioning::MsgTypes::kWiFiAssociationRequest: {
        char SSID[chip::DeviceLayer::Internal::kMaxWiFiSSIDLength];
        char passwd[chip::DeviceLayer::Internal::kMaxWiFiKeyLength];
        BufBound bbufSSID(Uint8::from_char(SSID), chip::DeviceLayer::Internal::kMaxWiFiSSIDLength);
        BufBound bbufPW(Uint8::from_char(passwd), chip::DeviceLayer::Internal::kMaxWiFiKeyLength);

        const uint8_t * buffer = msgBuf->Start();
        size_t len             = msgBuf->DataLength();
        size_t offset          = 0;

        ChipLogProgress(NetworkProvisioning, "Received kWiFiAssociationRequest. DeviceDelegate %p", mDeviceDelegate);

        VerifyOrExit(mDeviceDelegate != nullptr, err = CHIP_ERROR_INCORRECT_STATE);

        err = DecodeString(&buffer[offset], len - offset, bbufSSID, offset);
        // TODO: Check for the error once network provisioning is moved to delegate calls

        err = DecodeString(&buffer[offset], len - offset, bbufPW, offset);
        // TODO: Check for the error once network provisioning is moved to delegate calls

#if CONFIG_DEVICE_LAYER
        // Start listening for Internet connectivity changes to be able to respond with assigned IP Address
        DeviceLayer::PlatformMgr().AddEventHandler(ConnectivityHandler, reinterpret_cast<intptr_t>(this));
#endif

        mDeviceDelegate->ProvisionWiFi(SSID, passwd);
        err = CHIP_NO_ERROR;
    }
    break;

    case NetworkProvisioning::MsgTypes::kThreadAssociationRequest:
        ChipLogProgress(NetworkProvisioning, "Received kThreadAssociationRequest");
        err = HandleThreadAssociationRequest(msgBuf);
        break;

    case NetworkProvisioning::MsgTypes::kIPAddressAssigned: {
        ChipLogProgress(NetworkProvisioning, "Received kIPAddressAssigned");
        if (!Inet::IPAddress::FromString(Uint8::to_const_char(msgBuf->Start()), msgBuf->DataLength(), mDeviceAddress))
        {
            ExitNow(err = CHIP_ERROR_INVALID_ADDRESS);
        }
    }
    break;

    default:
        ExitNow(err = CHIP_ERROR_INVALID_MESSAGE_TYPE);
        break;
    };

exit:
    if (mDelegate != nullptr)
    {
        if (err != CHIP_NO_ERROR)
        {
            ChipLogError(NetworkProvisioning, "Failed in HandleNetworkProvisioningMessage. error %s\n", ErrorStr(err));
            mDelegate->OnNetworkProvisioningError(err);
        }
        else
        {
            // Network provisioning handshake requires only one message exchange in either direction.
            // If the current message handling did not result in an error, network provisioning is
            // complete.
            mDelegate->OnNetworkProvisioningComplete();
        }
    }
    return err;
}

CHIP_ERROR NetworkProvisioning::EncodeString(const char * str, BufBound & bbuf)
{
    CHIP_ERROR err  = CHIP_NO_ERROR;
    size_t length   = strlen(str);
    uint16_t u16len = static_cast<uint16_t>(length);
    VerifyOrExit(CanCastTo<uint16_t>(length), err = CHIP_ERROR_INVALID_ARGUMENT);

    bbuf.PutLE16(u16len);
    bbuf.Put(str);

exit:
    return err;
}

CHIP_ERROR NetworkProvisioning::DecodeString(const uint8_t * input, size_t input_len, BufBound & bbuf, size_t & consumed)
{
    CHIP_ERROR err  = CHIP_NO_ERROR;
    uint16_t length = 0;

    VerifyOrExit(input_len >= sizeof(uint16_t), err = CHIP_ERROR_BUFFER_TOO_SMALL);
    length   = chip::Encoding::LittleEndian::Get16(input);
    consumed = sizeof(uint16_t);

    VerifyOrExit(input_len - consumed >= length, err = CHIP_ERROR_BUFFER_TOO_SMALL);
    bbuf.Put(&input[consumed], length);

    consumed += bbuf.Written();
    bbuf.Put('\0');

    VerifyOrExit(bbuf.Fit(), err = CHIP_ERROR_BUFFER_TOO_SMALL);

exit:
    return err;
}

CHIP_ERROR NetworkProvisioning::SendIPAddress(const Inet::IPAddress & addr)
{
    CHIP_ERROR err                = CHIP_NO_ERROR;
    System::PacketBuffer * buffer = System::PacketBuffer::New();
    char * addrStr                = addr.ToString(Uint8::to_char(buffer->Start()), buffer->AvailableDataLength());
    size_t addrLen                = 0;

    ChipLogProgress(NetworkProvisioning, "Sending IP Address. Delegate %p\n", mDelegate);
    VerifyOrExit(mDelegate != nullptr, err = CHIP_ERROR_INCORRECT_STATE);
    VerifyOrExit(addrStr != nullptr, err = CHIP_ERROR_INVALID_ADDRESS);

    addrLen = strlen(addrStr) + 1;

    VerifyOrExit(CanCastTo<uint16_t>(addrLen), err = CHIP_ERROR_INVALID_ARGUMENT);
    buffer->SetDataLength(static_cast<uint16_t>(addrLen));

    err = mDelegate->SendSecureMessage(Protocols::kChipProtocol_NetworkProvisioning,
                                       NetworkProvisioning::MsgTypes::kIPAddressAssigned, buffer);
    SuccessOrExit(err);

exit:
    if (CHIP_NO_ERROR != err)
    {
        ChipLogError(NetworkProvisioning, "Failed in sending IP address. error %s\n", ErrorStr(err));
        System::PacketBuffer::Free(buffer);
    }
    return err;
}

CHIP_ERROR NetworkProvisioning::SendNetworkCredentials(const char * ssid, const char * passwd)
{
    CHIP_ERROR err                = CHIP_NO_ERROR;
    System::PacketBuffer * buffer = System::PacketBuffer::New();
    BufBound bbuf(buffer->Start(), buffer->AvailableDataLength());

    ChipLogProgress(NetworkProvisioning, "Sending Network Creds. Delegate %p\n", mDelegate);
    VerifyOrExit(mDelegate != nullptr, err = CHIP_ERROR_INCORRECT_STATE);
    SuccessOrExit(EncodeString(ssid, bbuf));
    SuccessOrExit(EncodeString(passwd, bbuf));
    VerifyOrExit(bbuf.Fit(), err = CHIP_ERROR_BUFFER_TOO_SMALL);

    VerifyOrExit(CanCastTo<uint16_t>(bbuf.Written()), err = CHIP_ERROR_INVALID_ARGUMENT);
    buffer->SetDataLength(static_cast<uint16_t>(bbuf.Written()));

    err = mDelegate->SendSecureMessage(Protocols::kChipProtocol_NetworkProvisioning,
                                       NetworkProvisioning::MsgTypes::kWiFiAssociationRequest, buffer);
    SuccessOrExit(err);

exit:
    if (CHIP_NO_ERROR != err)
    {
        ChipLogError(NetworkProvisioning, "Failed in sending Network Creds. error %s\n", ErrorStr(err));
        System::PacketBuffer::Free(buffer);
    }
    return err;
}

#ifdef CHIP_ENABLE_OPENTHREAD
CHIP_ERROR NetworkProvisioning::ParseThreadActiveOperationalDatasetTLVs(const uint8_t *threadTLVs, size_t threadTLVsLength,
    DeviceLayer::Internal::DeviceNetworkInfo & networkInfo)
{
    static constexpr uint16_t kThreadTlvExtendedLength    = 0xFF;
    static constexpr uint8_t  kThreadTlvTypeChannel       = 0;
    static constexpr uint8_t  kThreadTlvTypePANId         = 1;
    static constexpr uint8_t  kThreadTlvTypeExtendedPANId = 2;
    static constexpr uint8_t  kThreadTlvTypeNetworkName   = 3;
    static constexpr uint8_t  kThreadTlvTypePSKc          = 4;
    static constexpr uint8_t  kThreadTlvTypeMasterKey     = 5;
    static constexpr uint8_t  kThreadTlvTypeMeshPrefix    = 7;

    CHIP_ERROR err = CHIP_ERROR_INVALID_DATA_LIST;
    DeviceLayer::Internal::DeviceNetworkInfo  localNetworkInfo;
    const uint8_t *cur = threadTLVs;
    const uint8_t *end = threadTLVs + threadTLVsLength;

    while (cur < end)
    {
        uint8_t type;
        uint16_t length;
        const uint8_t *data;

        type = cur[0];
        ++cur;

        VerifyOrExit(cur < end, err = CHIP_ERROR_INVALID_DATA_LIST);
        if (cur[0] == kThreadTlvExtendedLength)
        {
            VerifyOrExit(cur + 1 < end, err = CHIP_ERROR_INVALID_DATA_LIST);
            length = Encoding::BigEndian::Get16(cur);
            cur += 2;
        }
        else
        {
            length = cur[0];
            ++cur;
        }

        VerifyOrExit(cur + length <= end, err = CHIP_ERROR_INVALID_DATA_LIST);
        data = cur;

        switch (type)
        {
        case kThreadTlvTypeChannel:
            VerifyOrExit(length == sizeof(uint8_t) + sizeof(localNetworkInfo.ThreadChannel), err = CHIP_ERROR_INVALID_DATA_LIST);
            // Skip Thread channel page.
            localNetworkInfo.ThreadChannel = Encoding::BigEndian::Get16(data + 1);
            break;
        case kThreadTlvTypePANId:
            VerifyOrExit(length == sizeof(localNetworkInfo.ThreadPANId), err = CHIP_ERROR_INVALID_DATA_LIST);
            localNetworkInfo.ThreadPANId = Encoding::BigEndian::Get16(data);
            break;
        case kThreadTlvTypeExtendedPANId:
            VerifyOrExit(length == sizeof(localNetworkInfo.ThreadExtendedPANId), err = CHIP_ERROR_INVALID_DATA_LIST);
            memcpy(localNetworkInfo.ThreadExtendedPANId, data, length);
            networkInfo.FieldPresent.ThreadExtendedPANId = true;
            break;
        case kThreadTlvTypeNetworkName:
            VerifyOrExit(length + 1U <= sizeof(localNetworkInfo.ThreadNetworkName), err = CHIP_ERROR_INVALID_DATA_LIST);
            memcpy(localNetworkInfo.ThreadNetworkName, data, length);
            localNetworkInfo.ThreadNetworkName[length] = '\0';
            break;
        case kThreadTlvTypePSKc:
            VerifyOrExit(length <= sizeof(localNetworkInfo.ThreadPSKc), err = CHIP_ERROR_INVALID_DATA_LIST);
            memcpy(localNetworkInfo.ThreadPSKc, data, length);
            networkInfo.FieldPresent.ThreadPSKc = true;
            break;
        case kThreadTlvTypeMasterKey:
            VerifyOrExit(length == sizeof(localNetworkInfo.ThreadMasterKey), err = CHIP_ERROR_INVALID_DATA_LIST);
            memcpy(localNetworkInfo.ThreadMasterKey, data, length);
            break;
        case kThreadTlvTypeMeshPrefix:
            VerifyOrExit(length == sizeof(localNetworkInfo.ThreadMeshPrefix), err = CHIP_ERROR_INVALID_DATA_LIST);
            memcpy(localNetworkInfo.ThreadMeshPrefix, data, length);
            networkInfo.FieldPresent.ThreadMeshPrefix = true;
            break;
        default:
            ChipLogProgress(NetworkProvisioning, "ignore unknown Thread TLV in Thread Provisioning message");
            break;
        }

        cur += length;
    }

    VerifyOrExit(cur == end, err = CHIP_ERROR_INVALID_DATA_LIST);
    localNetworkInfo.ThreadActiveOperationalDatasetTlvs.Alloc(threadTLVsLength);
    memcpy(localNetworkInfo.ThreadActiveOperationalDatasetTlvs.Get(), threadTLVs, threadTLVsLength);
    networkInfo = std::move(localNetworkInfo);

exit:
    return err;
}

CHIP_ERROR NetworkProvisioning::HandleThreadAssociationRequest(System::PacketBuffer * msgBuf)
{
    CHIP_ERROR err                                       = CHIP_NO_ERROR;
    DeviceLayer::Internal::DeviceNetworkInfo networkInfo = {};
    uint8_t * data                                       = msgBuf->Start();
    size_t dataLen                                       = msgBuf->DataLength();

    VerifyOrExit(mDeviceDelegate != nullptr, err = CHIP_ERROR_INCORRECT_STATE);

    SuccessOrExit(err = ParseThreadActiveOperationalDatasetTLVs(data, dataLen, networkInfo));
    networkInfo.NetworkId              = 0;
    networkInfo.FieldPresent.NetworkId = true;

#if CONFIG_DEVICE_LAYER
    // Start listening for OpenThread changes to be able to respond with SLAAC/On-Mesh IP Address
    DeviceLayer::PlatformMgr().AddEventHandler(ConnectivityHandler, reinterpret_cast<intptr_t>(this));
#endif

    mDeviceDelegate->ProvisionThread(networkInfo);
exit:
    return err;
}
#else  // CHIP_ENABLE_OPENTHREAD
CHIP_ERROR NetworkProvisioning::HandleThreadAssociationRequest(System::PacketBuffer *)
{
    return CHIP_ERROR_INVALID_MESSAGE_TYPE;
}
#endif // CHIP_ENABLE_OPENTHREAD

#if CONFIG_DEVICE_LAYER
void NetworkProvisioning::ConnectivityHandler(const DeviceLayer::ChipDeviceEvent * event, intptr_t arg)
{
    NetworkProvisioning * session = reinterpret_cast<NetworkProvisioning *>(arg);

    VerifyOrExit(session != nullptr, /**/);

    if (event->Type == DeviceLayer::DeviceEventType::kInternetConnectivityChange &&
        event->InternetConnectivityChange.IPv4 == DeviceLayer::kConnectivity_Established)
    {
        Inet::IPAddress addr;
        Inet::IPAddress::FromString(event->InternetConnectivityChange.address, addr);
        (void) session->SendIPAddress(addr);
    }

#if CHIP_DEVICE_CONFIG_ENABLE_THREAD
    if (event->Type == DeviceLayer::DeviceEventType::kThreadStateChange && event->ThreadStateChange.AddressChanged)
    {
        Inet::IPAddress addr;
        SuccessOrExit(DeviceLayer::ThreadStackMgr().GetSlaacIPv6Address(addr));
        (void) session->SendIPAddress(addr);
    }
#endif

exit:
    return;
}
#endif

} // namespace chip
