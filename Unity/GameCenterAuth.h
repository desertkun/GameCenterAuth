
#ifndef GameCenterAuth_h
#define GameCenterAuth_h

typedef void ( __stdcall *GenerateSucceeded )(
    const char* publicKeyUrl,
    uint64_t timestamp,
    const char* signature,
    const char* salt,
    const char* playerID,
    const char* alias,
    const char* bundleID
);

typedef void ( __stdcall *GenerateFailed )(
    const char* reason
);

extern "C"
{
    void GenerateIdentityVerificationSignature(GenerateSucceeded OnSucceeded, GenerateFailed OnFailed);
}

#endif
