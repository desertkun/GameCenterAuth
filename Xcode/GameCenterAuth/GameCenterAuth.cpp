
#include <GameKit/GameKit.h>

#include "GameCenterAuth.h"

void GenerateIdentityVerificationSignature(GenerateSucceeded OnSucceeded, GenerateFailed OnFailed)
{
    __weak GKLocalPlayer *localPlayer = [GKLocalPlayer localPlayer];
    
    [localPlayer generateIdentityVerificationSignatureWithCompletionHandler:
        ^(NSURL *publicKeyUrl, NSData *signature, NSData *salt, uint64_t timestamp, NSError *error)
        {
            if (error)
            {
                NSLog(@"ERROR: %@", error);
                OnFailed([[error localizedDescription] UTF8String]);
            }
            else
            {
                NSString *signatureb64 = [signature base64EncodedStringWithOptions:0];
                NSString *saltb64 = [salt base64EncodedStringWithOptions:0];
                NSString *playerId = localPlayer.playerID;
                NSString *alias = localPlayer.alias;
                NSString *bundleId = [[NSBundle mainBundle] bundleIdentifier];
     
                OnSucceeded(
                    [[publicKeyUrl absoluteString] UTF8String],
                    timestamp,
                    [signatureb64 UTF8String],
                    [saltb64 UTF8String],
                    [playerId UTF8String],
                    [alias UTF8String],
                    [bundleId UTF8String]
                );
            }
        }
     ];
}