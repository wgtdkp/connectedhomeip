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

/**
 *    @file
 *      This file implements a unit test suite for the Configuration Manager
 *      code functionality.
 *
 */

#include "TestConfigurationMgr.h"

#include <inttypes.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <nlunit-test.h>
#include <support/CodeUtils.h>
#include <support/TestUtils.h>

#include <platform/CHIPDeviceLayer.h>

using namespace chip;
using namespace chip::Logging;
using namespace chip::Inet;
using namespace chip::DeviceLayer;

// =================================
//      Unit tests
// =================================

static void TestPlatformMgr_Init(nlTestSuite * inSuite, void * inContext)
{
    // ConfigurationManager is initialized from PlatformManager indirectly
    CHIP_ERROR err = PlatformMgr().InitChipStack();
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);
}

#if defined(DEBUG)
static void TestPlatformMgr_RunUnitTest(nlTestSuite * inSuite, void * inContext)
{
    CHIP_ERROR err = CHIP_NO_ERROR;

    err = ConfigurationMgr().RunUnitTests();
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);
}
#endif

static void TestConfigurationMgr_SerialNumber(nlTestSuite * inSuite, void * inContext)
{
    CHIP_ERROR err = CHIP_NO_ERROR;

    char buf[64];
    size_t serialNumberLen    = 0;
    const char * serialNumber = "89051AAZZ236";

    err = ConfigurationMgr().StoreSerialNumber(serialNumber, strlen(serialNumber));
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    err = ConfigurationMgr().GetSerialNumber(buf, 64, serialNumberLen);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    NL_TEST_ASSERT(inSuite, serialNumberLen == strlen(serialNumber));
    NL_TEST_ASSERT(inSuite, strcmp(buf, serialNumber) == 0);

    err = ConfigurationMgr().StoreSerialNumber(serialNumber, 5);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    err = ConfigurationMgr().GetSerialNumber(buf, 64, serialNumberLen);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    NL_TEST_ASSERT(inSuite, serialNumberLen == 5);
    NL_TEST_ASSERT(inSuite, strcmp(buf, "89051") == 0);
}

static void TestConfigurationMgr_ManufacturingDate(nlTestSuite * inSuite, void * inContext)
{
    CHIP_ERROR err = CHIP_NO_ERROR;

    const char * mfgDate = "2008/09/20";
    uint16_t year;
    uint8_t month;
    uint8_t dayOfMonth;

    err = ConfigurationMgr().StoreManufacturingDate(mfgDate, strlen(mfgDate));
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    err = ConfigurationMgr().GetManufacturingDate(year, month, dayOfMonth);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    NL_TEST_ASSERT(inSuite, year == 2008);
    NL_TEST_ASSERT(inSuite, month == 9);
    NL_TEST_ASSERT(inSuite, dayOfMonth == 20);
}

static void TestConfigurationMgr_ProductRevision(nlTestSuite * inSuite, void * inContext)
{
    CHIP_ERROR err = CHIP_NO_ERROR;
    uint16_t productRev;

    err = ConfigurationMgr().StoreProductRevision(1234);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    err = ConfigurationMgr().GetProductRevision(productRev);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    NL_TEST_ASSERT(inSuite, productRev == 1234);
}

static void TestConfigurationMgr_ManufacturerDeviceId(nlTestSuite * inSuite, void * inContext)
{
    CHIP_ERROR err = CHIP_NO_ERROR;
    uint64_t deviceId;

    err = ConfigurationMgr().StoreManufacturerDeviceId(7212064004600625234);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    err = ConfigurationMgr().GetManufacturerDeviceId(deviceId);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    NL_TEST_ASSERT(inSuite, deviceId == 7212064004600625234);
}

static void TestConfigurationMgr_ManufacturerDeviceCertificate(nlTestSuite * inSuite, void * inContext)
{
    CHIP_ERROR err              = CHIP_NO_ERROR;
    const static uint8_t cert[] = {
        0xD5, 0x00, 0x00, 0x04, 0x00, 0x01, 0x00, 0x30, 0x01, 0x08, 0x79, 0x55, 0x9F, 0x15, 0x1F, 0x66, 0x3D, 0x8F, 0x24,
        0x02, 0x05, 0x37, 0x03, 0x27, 0x13, 0x02, 0x00, 0x00, 0xEE, 0xEE, 0x30, 0xB4, 0x18, 0x18, 0x26, 0x04, 0x80, 0x41,
        0x1B, 0x23, 0x26, 0x05, 0x7F, 0xFF, 0xFF, 0x52, 0x37, 0x06, 0x27, 0x11, 0x01, 0x00, 0x00, 0x00, 0x00, 0x30, 0xB4,
        0x18, 0x18, 0x24, 0x07, 0x02, 0x26, 0x08, 0x25, 0x00, 0x5A, 0x23, 0x30, 0x0A, 0x39, 0x04, 0x9E, 0xC7, 0x77, 0xC5,
        0xA4, 0x13, 0x31, 0xF7, 0x72, 0x2E, 0x27, 0xC2, 0x86, 0x3D, 0xC5, 0x2E, 0xD5, 0xD2, 0x3C, 0xCF, 0x7E, 0x06, 0xE3,
        0x48, 0x53, 0x87, 0xE8, 0x4D, 0xB0, 0x27, 0x07, 0x58, 0x4A, 0x38, 0xB4, 0xF3, 0xB2, 0x47, 0x94, 0x45, 0x58, 0x65,
        0x80, 0x08, 0x17, 0x6B, 0x8E, 0x4F, 0x07, 0x41, 0xA3, 0x3D, 0x5D, 0xCE, 0x76, 0x86, 0x35, 0x83, 0x29, 0x01, 0x18,
        0x35, 0x82, 0x29, 0x01, 0x24, 0x02, 0x05, 0x18, 0x35, 0x84, 0x29, 0x01, 0x36, 0x02, 0x04, 0x02, 0x04, 0x01, 0x18,
        0x18, 0x35, 0x81, 0x30, 0x02, 0x08, 0x42, 0xBD, 0x2C, 0x6B, 0x5B, 0x3A, 0x18, 0x16, 0x18, 0x35, 0x80, 0x30, 0x02,
        0x08, 0x44, 0xE3, 0x40, 0x38, 0xA9, 0xD4, 0xB5, 0xA7, 0x18, 0x35, 0x0C, 0x30, 0x01, 0x19, 0x00, 0xA6, 0x5D, 0x54,
        0xF5, 0xAE, 0x5D, 0x63, 0xEB, 0x69, 0xD8, 0xDB, 0xCB, 0xE2, 0x20, 0x0C, 0xD5, 0x6F, 0x43, 0x5E, 0x96, 0xA8, 0x54,
        0xB2, 0x74, 0x30, 0x02, 0x19, 0x00, 0xE0, 0x37, 0x02, 0x8B, 0xB3, 0x04, 0x06, 0xDD, 0xBD, 0x28, 0xAA, 0xC4, 0xF1,
        0xFF, 0xFB, 0xB1, 0xD4, 0x1C, 0x78, 0x40, 0xDA, 0x2C, 0xD8, 0x40, 0x18, 0x18,
    };
    uint8_t buf[512];
    size_t certLen;

    err = ConfigurationMgr().StoreManufacturerDeviceCertificate(cert, sizeof(cert));
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    err = ConfigurationMgr().GetManufacturerDeviceCertificate(buf, sizeof(buf), certLen);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    NL_TEST_ASSERT(inSuite, certLen == sizeof(cert));
    NL_TEST_ASSERT(inSuite, memcmp(buf, cert, certLen) == 0);
}

static void TestConfigurationMgr_ManufacturerDeviceIntermediateCACerts(nlTestSuite * inSuite, void * inContext)
{
    CHIP_ERROR err               = CHIP_NO_ERROR;
    const static uint8_t certs[] = {
        0xD5, 0x00, 0x00, 0x04, 0x00, 0x01, 0x00, 0x30, 0x01, 0x08, 0x79, 0x55, 0x9F, 0x15, 0x1F, 0x66, 0x3D, 0x8F, 0x24,
        0x02, 0x05, 0x37, 0x03, 0x27, 0x13, 0x02, 0x00, 0x00, 0xEE, 0xEE, 0x30, 0xB4, 0x18, 0x18, 0x26, 0x04, 0x80, 0x41,
        0x1B, 0x23, 0x26, 0x05, 0x7F, 0xFF, 0xFF, 0x52, 0x37, 0x06, 0x27, 0x11, 0x01, 0x00, 0x00, 0x00, 0x00, 0x30, 0xB4,
        0x18, 0x18, 0x24, 0x07, 0x02, 0x26, 0x08, 0x25, 0x00, 0x5A, 0x23, 0x30, 0x0A, 0x39, 0x04, 0x9E, 0xC7, 0x77, 0xC5,
        0xA4, 0x13, 0x31, 0xF7, 0x72, 0x2E, 0x27, 0xC2, 0x86, 0x3D, 0xC5, 0x2E, 0xD5, 0xD2, 0x3C, 0xCF, 0x7E, 0x06, 0xE3,
        0x48, 0x53, 0x87, 0xE8, 0x4D, 0xB0, 0x27, 0x07, 0x58, 0x4A, 0x38, 0xB4, 0xF3, 0xB2, 0x47, 0x94, 0x45, 0x58, 0x65,
        0x80, 0x08, 0x17, 0x6B, 0x8E, 0x4F, 0x07, 0x41, 0xA3, 0x3D, 0x5D, 0xCE, 0x76, 0x86, 0x35, 0x83, 0x29, 0x01, 0x18,
        0x35, 0x82, 0x29, 0x01, 0x24, 0x02, 0x05, 0x18, 0x35, 0x84, 0x29, 0x01, 0x36, 0x02, 0x04, 0x02, 0x04, 0x01, 0x18,
        0x18, 0x35, 0x81, 0x30, 0x02, 0x08, 0x42, 0xBD, 0x2C, 0x6B, 0x5B, 0x3A, 0x18, 0x16, 0x18, 0x35, 0x80, 0x30, 0x02,
        0x08, 0x44, 0xE3, 0x40, 0x38, 0xA9, 0xD4, 0xB5, 0xA7, 0x18, 0x35, 0x0C, 0x30, 0x01, 0x19, 0x00, 0xA6, 0x5D, 0x54,
        0xF5, 0xAE, 0x5D, 0x63, 0xEB, 0x69, 0xD8, 0xDB, 0xCB, 0xE2, 0x20, 0x0C, 0xD5, 0x6F, 0x43, 0x5E, 0x96, 0xA8, 0x54,
        0xB2, 0x74, 0x30, 0x02, 0x19, 0x00, 0xE0, 0x37, 0x02, 0x8B, 0xB3, 0x04, 0x06, 0xDD, 0xBD, 0x28, 0xAA, 0xC4, 0xF1,
        0xFF, 0xFB, 0xB1, 0xD4, 0x1C, 0x78, 0x40, 0xDA, 0x2C, 0xD8, 0x40, 0x18, 0x18,
    };
    uint8_t buf[512];
    size_t certsLen;

    err = ConfigurationMgr().StoreManufacturerDeviceCertificate(certs, sizeof(certs));
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    err = ConfigurationMgr().GetManufacturerDeviceCertificate(buf, sizeof(buf), certsLen);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    NL_TEST_ASSERT(inSuite, certsLen == sizeof(certs));
    NL_TEST_ASSERT(inSuite, memcmp(buf, certs, certsLen) == 0);
}

static void TestConfigurationMgr_ManufacturerDevicePrivateKey(nlTestSuite * inSuite, void * inContext)
{
    CHIP_ERROR err             = CHIP_NO_ERROR;
    const static uint8_t key[] = {
        0x80, 0x08, 0x17, 0x6B, 0x8E, 0x4F, 0x07, 0x41, 0xA3, 0x3D, 0x5D, 0xCE, 0x76, 0x86, 0x35, 0x83, 0x29, 0x01, 0x18,
        0x35, 0x82, 0x29, 0x01, 0x24, 0x02, 0x05, 0x18, 0x35, 0x84, 0x29, 0x01, 0x36, 0x02, 0x04, 0x02, 0x04, 0x01, 0x18,
        0x18, 0x35, 0x81, 0x30, 0x02, 0x08, 0x42, 0xBD, 0x2C, 0x6B, 0x5B, 0x3A, 0x18, 0x16, 0x18, 0x35, 0x80, 0x30, 0x02,
    };
    uint8_t buf[128];
    size_t keyLen;

    err = ConfigurationMgr().StoreManufacturerDevicePrivateKey(key, sizeof(key));
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    err = ConfigurationMgr().GetManufacturerDevicePrivateKey(buf, sizeof(buf), keyLen);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    NL_TEST_ASSERT(inSuite, keyLen == sizeof(key));
    NL_TEST_ASSERT(inSuite, memcmp(buf, key, keyLen) == 0);
}

static void TestConfigurationMgr_FabricId(nlTestSuite * inSuite, void * inContext)
{
#if CHIP_CONFIG_ENABLE_FABRIC_STATE
    CHIP_ERROR err    = CHIP_NO_ERROR;
    uint64_t fabricId = 0;

    err = ConfigurationMgr().StoreFabricId(2006255910626445ULL);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    err = ConfigurationMgr().GetFabricId(fabricId);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    NL_TEST_ASSERT(inSuite, fabricId == 2006255910626445ULL);
#endif
}

static void TestConfigurationMgr_ServiceConfig(nlTestSuite * inSuite, void * inContext)
{
    CHIP_ERROR err = CHIP_NO_ERROR;

    uint8_t buf[1024];
    size_t serviceConfigLen              = 0;
    const static uint8_t serviceConfig[] = {
        0x1B, 0x23, 0x26, 0x05, 0x7F, 0xFF, 0xFF, 0x52, 0x37, 0x06, 0x27, 0x11, 0x01, 0x00, 0x00, 0x00, 0x00, 0x30, 0xB4, 0x18,
        0x18, 0x24, 0x07, 0x02, 0x26, 0x08, 0x25, 0x00, 0x5A, 0x23, 0x30, 0x0A, 0x39, 0x04, 0x9E, 0xC7, 0x77, 0xC5, 0xA4, 0x13,
        0x31, 0xF7, 0x72, 0x2E, 0x27, 0xC2, 0x86, 0x3D, 0xC5, 0x2E, 0xD5, 0xD2, 0x3C, 0xCF, 0x7E, 0x06, 0xE3, 0x48, 0x53, 0x87,
        0xE8, 0x4D, 0xB0, 0x27, 0x07, 0x58, 0x4A, 0x38, 0xB4, 0xF3, 0xB2, 0x47, 0x94, 0x45, 0x58, 0x65, 0x80, 0x08, 0x17, 0x6B,
        0x8E, 0x4F, 0x07, 0x41, 0xA3, 0x3D, 0x5D, 0xCE, 0x76, 0x86, 0x35, 0x83, 0x29, 0x01, 0x18, 0x35, 0x82, 0x29, 0x01, 0x24,
        0x02, 0x05, 0x18, 0x35, 0x84, 0x29, 0x01, 0x36, 0x02, 0x04, 0x02, 0x04, 0x01, 0x18, 0x18, 0x35, 0x81, 0x30, 0x02, 0x08,
        0x42, 0xBD, 0x2C, 0x6B, 0x5B, 0x3A, 0x18, 0x16, 0x18, 0x35, 0x80, 0x30, 0x02, 0x08, 0x44, 0xE3, 0x40, 0x38, 0xA9, 0xD4,
        0xB5, 0xA7, 0x18, 0x35, 0x0C, 0x30, 0x01, 0x19, 0x00, 0xA6, 0x5D, 0x54, 0xD5, 0x00, 0x00, 0x04, 0x00, 0x01, 0x00, 0x30,
        0x01, 0x08, 0x79, 0x55, 0x9F, 0x15, 0x1F, 0x66, 0x3D, 0x8F, 0x24, 0x02, 0x05, 0x37, 0x03, 0x27, 0x13, 0x02, 0x00, 0x00,
        0xEE, 0xEE, 0x30, 0xB4, 0x18, 0x18, 0x26, 0x04, 0x80, 0x41, 0x1B, 0x23, 0x26, 0x05, 0x7F, 0xFF, 0xFF, 0x52, 0x37, 0x06,
        0x27, 0x11, 0x01, 0x00, 0x00, 0x00, 0x00, 0x30, 0xB4, 0x18, 0x18, 0x24, 0x07, 0x02, 0x26, 0x08, 0x25, 0x00, 0x5A, 0x23,
        0x30, 0x0A, 0x39, 0x04, 0x9E, 0xC7, 0x77, 0xC5, 0xA4, 0x13, 0x31, 0xF7, 0x72, 0x2E, 0x27, 0xC2, 0x86, 0x3D, 0xC5, 0x2E,
        0xD5, 0xD2, 0x3C, 0xCF, 0x7E, 0x06, 0xE3, 0x48, 0x53, 0x87, 0xE8, 0x4D, 0xB0, 0x27, 0x07, 0x58, 0x4A, 0x38, 0xB4, 0xF3,
        0xB2, 0x47, 0x94, 0x45, 0x58, 0x65, 0x80, 0x08, 0x17, 0x6B, 0x8E, 0x4F, 0x07, 0x41, 0xA3, 0x3D, 0x5D, 0xCE, 0x76, 0x86,
        0x35, 0x83, 0x29, 0x01, 0x18, 0x35, 0x82, 0x29, 0x01, 0x24, 0x02, 0x05, 0x18, 0x35, 0x84, 0x29, 0x01, 0x36, 0x02, 0x04,
        0x02, 0x04, 0x01, 0x18, 0x18, 0x35, 0x81, 0x30, 0x02, 0x08, 0x42, 0xBD, 0x2C, 0x6B, 0x5B, 0x3A, 0x18, 0x16, 0x18, 0x35,
        0x80, 0x30, 0x02, 0x08, 0x44, 0xE3, 0x40, 0x38, 0xA9, 0xD4, 0xB5, 0xA7, 0x18, 0x35, 0x0C, 0x30, 0x01, 0x19, 0x00, 0xA6,
        0x5D, 0x54, 0xF5, 0xAE, 0x5D, 0x63, 0xEB, 0x69, 0xD8, 0xDB, 0xCB, 0xE2, 0x20, 0x0C, 0xD5, 0x6F, 0x43, 0x5E, 0x96, 0xA8,
        0x54, 0xB2, 0x74, 0x30, 0x02, 0x19, 0x00, 0xE0, 0x37, 0x02, 0x8B, 0xB3, 0x04, 0x06, 0xDD, 0xBD, 0x28, 0xAA, 0xC4, 0xF1,
        0xFF, 0xFB, 0xB1, 0xD4, 0x1C, 0x78, 0x40, 0xDA, 0x2C, 0xD8, 0x40, 0x18, 0x18, 0x18, 0x02, 0x40
    };
    err = ConfigurationMgr().StoreServiceConfig(serviceConfig, sizeof(serviceConfig));
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    err = ConfigurationMgr().GetServiceConfig(buf, 1024, serviceConfigLen);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    NL_TEST_ASSERT(inSuite, serviceConfigLen == sizeof(serviceConfig));
    NL_TEST_ASSERT(inSuite, memcmp(buf, serviceConfig, serviceConfigLen) == 0);
}

static void TestConfigurationMgr_SetupPinCode(nlTestSuite * inSuite, void * inContext)
{
    CHIP_ERROR err = CHIP_NO_ERROR;

    const uint32_t setSetupPinCode = 34567890;
    uint32_t getSetupPinCode       = 0;

    err = ConfigurationMgr().StoreSetupPinCode(setSetupPinCode);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    err = ConfigurationMgr().GetSetupPinCode(getSetupPinCode);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    NL_TEST_ASSERT(inSuite, getSetupPinCode == setSetupPinCode);
}

static void TestConfigurationMgr_SetupDiscriminator(nlTestSuite * inSuite, void * inContext)
{
    CHIP_ERROR err = CHIP_NO_ERROR;

    const uint32_t setSetupDiscriminator = 0xBA0;
    uint32_t getSetupDiscriminator       = 0;

    err = ConfigurationMgr().StoreSetupDiscriminator(setSetupDiscriminator);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    err = ConfigurationMgr().GetSetupDiscriminator(getSetupDiscriminator);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    NL_TEST_ASSERT(inSuite, getSetupDiscriminator == setSetupDiscriminator);
}

static void TestConfigurationMgr_PairedAccountId(nlTestSuite * inSuite, void * inContext)
{
    CHIP_ERROR err = CHIP_NO_ERROR;

    char buf[64];
    size_t accountIdLen    = 0;
    const char * accountId = "USER_016CB664A86A888D";

    err = ConfigurationMgr().StorePairedAccountId(accountId, strlen(accountId));
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    err = ConfigurationMgr().GetPairedAccountId(buf, 64, accountIdLen);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    NL_TEST_ASSERT(inSuite, accountIdLen == strlen(accountId));
    NL_TEST_ASSERT(inSuite, strcmp(buf, "USER_016CB664A86A888D") == 0);
}

static void TestConfigurationMgr_ServiceProvisioningData(nlTestSuite * inSuite, void * inContext)
{
    CHIP_ERROR err = CHIP_NO_ERROR;

    uint64_t serviceId;

    char account[64];
    size_t accountIdLen    = 0;
    const char * accountId = "USER_016CB664A86A888D";

    uint8_t buf[1024];
    size_t serviceConfigLen              = 0;
    const static uint8_t serviceConfig[] = {
        0x1B, 0x23, 0x26, 0x05, 0x7F, 0xFF, 0xFF, 0x52, 0x37, 0x06, 0x27, 0x11, 0x01, 0x00, 0x00, 0x00, 0x00, 0x30, 0xB4, 0x18,
        0x18, 0x24, 0x07, 0x02, 0x26, 0x08, 0x25, 0x00, 0x5A, 0x23, 0x30, 0x0A, 0x39, 0x04, 0x9E, 0xC7, 0x77, 0xC5, 0xA4, 0x13,
        0x31, 0xF7, 0x72, 0x2E, 0x27, 0xC2, 0x86, 0x3D, 0xC5, 0x2E, 0xD5, 0xD2, 0x3C, 0xCF, 0x7E, 0x06, 0xE3, 0x48, 0x53, 0x87,
        0xE8, 0x4D, 0xB0, 0x27, 0x07, 0x58, 0x4A, 0x38, 0xB4, 0xF3, 0xB2, 0x47, 0x94, 0x45, 0x58, 0x65, 0x80, 0x08, 0x17, 0x6B,
        0x8E, 0x4F, 0x07, 0x41, 0xA3, 0x3D, 0x5D, 0xCE, 0x76, 0x86, 0x35, 0x83, 0x29, 0x01, 0x18, 0x35, 0x82, 0x29, 0x01, 0x24,
        0x02, 0x05, 0x18, 0x35, 0x84, 0x29, 0x01, 0x36, 0x02, 0x04, 0x02, 0x04, 0x01, 0x18, 0x18, 0x35, 0x81, 0x30, 0x02, 0x08,
        0x42, 0xBD, 0x2C, 0x6B, 0x5B, 0x3A, 0x18, 0x16, 0x18, 0x35, 0x80, 0x30, 0x02, 0x08, 0x44, 0xE3, 0x40, 0x38, 0xA9, 0xD4,
        0xB5, 0xA7, 0x18, 0x35, 0x0C, 0x30, 0x01, 0x19, 0x00, 0xA6, 0x5D, 0x54, 0xD5, 0x00, 0x00, 0x04, 0x00, 0x01, 0x00, 0x30,
        0x01, 0x08, 0x79, 0x55, 0x9F, 0x15, 0x1F, 0x66, 0x3D, 0x8F, 0x24, 0x02, 0x05, 0x37, 0x03, 0x27, 0x13, 0x02, 0x00, 0x00,
        0xEE, 0xEE, 0x30, 0xB4, 0x18, 0x18, 0x26, 0x04, 0x80, 0x41, 0x1B, 0x23, 0x26, 0x05, 0x7F, 0xFF, 0xFF, 0x52, 0x37, 0x06,
        0x27, 0x11, 0x01, 0x00, 0x00, 0x00, 0x00, 0x30, 0xB4, 0x18, 0x18, 0x24, 0x07, 0x02, 0x26, 0x08, 0x25, 0x00, 0x5A, 0x23,
        0x30, 0x0A, 0x39, 0x04, 0x9E, 0xC7, 0x77, 0xC5, 0xA4, 0x13, 0x31, 0xF7, 0x72, 0x2E, 0x27, 0xC2, 0x86, 0x3D, 0xC5, 0x2E,
        0xD5, 0xD2, 0x3C, 0xCF, 0x7E, 0x06, 0xE3, 0x48, 0x53, 0x87, 0xE8, 0x4D, 0xB0, 0x27, 0x07, 0x58, 0x4A, 0x38, 0xB4, 0xF3,
        0xB2, 0x47, 0x94, 0x45, 0x58, 0x65, 0x80, 0x08, 0x17, 0x6B, 0x8E, 0x4F, 0x07, 0x41, 0xA3, 0x3D, 0x5D, 0xCE, 0x76, 0x86,
        0x35, 0x83, 0x29, 0x01, 0x18, 0x35, 0x82, 0x29, 0x01, 0x24, 0x02, 0x05, 0x18, 0x35, 0x84, 0x29, 0x01, 0x36, 0x02, 0x04,
        0x02, 0x04, 0x01, 0x18, 0x18, 0x35, 0x81, 0x30, 0x02, 0x08, 0x42, 0xBD, 0x2C, 0x6B, 0x5B, 0x3A, 0x18, 0x16, 0x18, 0x35,
        0x80, 0x30, 0x02, 0x08, 0x44, 0xE3, 0x40, 0x38, 0xA9, 0xD4, 0xB5, 0xA7, 0x18, 0x35, 0x0C, 0x30, 0x01, 0x19, 0x00, 0xA6,
        0x5D, 0x54, 0xF5, 0xAE, 0x5D, 0x63, 0xEB, 0x69, 0xD8, 0xDB, 0xCB, 0xE2, 0x20, 0x0C, 0xD5, 0x6F, 0x43, 0x5E, 0x96, 0xA8,
        0x54, 0xB2, 0x74, 0x30, 0x02, 0x19, 0x00, 0xE0, 0x37, 0x02, 0x8B, 0xB3, 0x04, 0x06, 0xDD, 0xBD, 0x28, 0xAA, 0xC4, 0xF1,
        0xFF, 0xFB, 0xB1, 0xD4, 0x1C, 0x78, 0x40, 0xDA, 0x2C, 0xD8, 0x40, 0x18, 0x18, 0x18, 0x02, 0x40
    };

    // Test write/clear
    err = ConfigurationMgr().StoreServiceProvisioningData(7212064004600625234, serviceConfig, sizeof(serviceConfig), accountId,
                                                          strlen(accountId));
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    err = ConfigurationMgr().ClearServiceProvisioningData();
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    err = ConfigurationMgr().GetServiceId(serviceId);
    NL_TEST_ASSERT(inSuite, err != CHIP_NO_ERROR);

    err = ConfigurationMgr().GetPairedAccountId(account, 64, accountIdLen);
    NL_TEST_ASSERT(inSuite, err != CHIP_NO_ERROR);

    err = ConfigurationMgr().GetServiceConfig(buf, 512, serviceConfigLen);
    NL_TEST_ASSERT(inSuite, err != CHIP_NO_ERROR);

    // Test write/read
    err = ConfigurationMgr().StoreServiceProvisioningData(7212064004600625234, serviceConfig, sizeof(serviceConfig), accountId,
                                                          strlen(accountId));
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);

    err = ConfigurationMgr().GetServiceId(serviceId);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);
    NL_TEST_ASSERT(inSuite, serviceId == 7212064004600625234);

    err = ConfigurationMgr().GetPairedAccountId(account, 64, accountIdLen);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);
    NL_TEST_ASSERT(inSuite, accountIdLen == strlen(accountId));
    NL_TEST_ASSERT(inSuite, strcmp(account, "USER_016CB664A86A888D") == 0);

    err = ConfigurationMgr().GetServiceConfig(buf, 512, serviceConfigLen);
    NL_TEST_ASSERT(inSuite, err == CHIP_NO_ERROR);
    NL_TEST_ASSERT(inSuite, serviceConfigLen == sizeof(serviceConfig));
    NL_TEST_ASSERT(inSuite, memcmp(buf, serviceConfig, serviceConfigLen) == 0);
}

/**
 *   Test Suite. It lists all the test functions.
 */
static const nlTest sTests[] = {

    NL_TEST_DEF("Test PlatformMgr::Init", TestPlatformMgr_Init),
#if defined(DEBUG)
    NL_TEST_DEF("Test PlatformMgr::RunUnitTest", TestPlatformMgr_RunUnitTest),
#endif
    NL_TEST_DEF("Test ConfigurationMgr::SerialNumber", TestConfigurationMgr_SerialNumber),
    NL_TEST_DEF("Test ConfigurationMgr::ManufacturingDate", TestConfigurationMgr_ManufacturingDate),
    NL_TEST_DEF("Test ConfigurationMgr::ProductRevision", TestConfigurationMgr_ProductRevision),
    NL_TEST_DEF("Test ConfigurationMgr::ManufacturerDeviceId", TestConfigurationMgr_ManufacturerDeviceId),
    NL_TEST_DEF("Test ConfigurationMgr::ManufacturerDeviceCertificate", TestConfigurationMgr_ManufacturerDeviceCertificate),
    NL_TEST_DEF("Test ConfigurationMgr::ManufacturerDeviceIntermediateCACerts",
                TestConfigurationMgr_ManufacturerDeviceIntermediateCACerts),
    NL_TEST_DEF("Test ConfigurationMgr::ManufacturerDevicePrivateKey", TestConfigurationMgr_ManufacturerDevicePrivateKey),
    NL_TEST_DEF("Test ConfigurationMgr::SetupPinCode", TestConfigurationMgr_SetupPinCode),
    NL_TEST_DEF("Test ConfigurationMgr::SetupDiscriminator", TestConfigurationMgr_SetupDiscriminator),
    NL_TEST_DEF("Test ConfigurationMgr::FabricId", TestConfigurationMgr_FabricId),
    NL_TEST_DEF("Test ConfigurationMgr::ServiceConfig", TestConfigurationMgr_ServiceConfig),
    NL_TEST_DEF("Test ConfigurationMgr::PairedAccountId", TestConfigurationMgr_PairedAccountId),
    NL_TEST_DEF("Test ConfigurationMgr::ServiceProvisioningData", TestConfigurationMgr_ServiceProvisioningData),
    NL_TEST_SENTINEL()
};

int TestConfigurationMgr(void)
{
    nlTestSuite theSuite = { "CHIP DeviceLayer time tests", &sTests[0], NULL, NULL };

    // Run test suit againt one context.
    nlTestRunner(&theSuite, NULL);
    return nlTestRunnerStats(&theSuite);
}

static void __attribute__((constructor)) TestConfigurationMgrCtor(void)
{
    VerifyOrDie(RegisterUnitTests(&TestConfigurationMgr) == CHIP_NO_ERROR);
}
