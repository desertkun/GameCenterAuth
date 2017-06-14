# GameCenterAuth
Unity Native implementation for Game Center server-side validation. Make possible to ensure the user's gamecenter ID is valid. Static library is already compiled (`libGameCenterAuth.a`), but you may want to recompile them from `Xcode` folder.

# How To Use
1. Copy `Unity/*` files (including `*.meta`) in your unity project
2. Authentificate using unity's standard method `Social.localUser.Authenticate` from package `UnityEngine.SocialPlatforms.GameCenter`.
3. Call `GameCenterSignature.Generate` with `OnSucceeded` and `OnFailed` callbacks.
4. In case of success, pass arguments from `OnSucceeded` to your server and [validate it server-side](https://developer.apple.com/library/ios/documentation/GameKit/Reference/GKLocalPlayer_Ref/#//apple_ref/occ/instm/GKLocalPlayer/generateIdentityVerificationSignatureWithCompletionHandler:)
5. Make sure the callbacks are static and have the `[MonoPInvokeCallback(typeof(GameCenterSignature.OnSucceeded))]` and `[MonoPInvokeCallback(typeof(GameCenterSignature.OnFailed))]` attributes.

# How To Recompile

```
xcodebuild -project Xcode/GameCenterAuth.xcodeproj -alltargets ARCHS='arm64 armv7 armv7s' IPHONEOS_DEPLOYMENT_TARGET='8.0'
```

# Usage Example

```
using UnityEngine;
using UnityEngine.SocialPlatforms.GameCenter;
using AOT;

namespace Online
{
    #if (UNITY_IOS)
    public class AuthorizeGameCenter : MonoBehaviour
    {
        [MonoPInvokeCallback(typeof(GameCenterSignature.OnSucceeded))]
        private static void OnSucceeded(
            string PublicKeyUrl, 
            ulong timestamp,
            string signature,
            string salt,
            string playerID,
            string alias,
            string bundleID)
        {
            Debug.Log("Succeeded authorization to gamecenter: \n" +
                "PublicKeyUrl=" + PublicKeyUrl + "\n" +
                "timestamp=" + timestamp + "\n" +
                "signature=" + signature + "\n" + 
                "salt=" + salt + "\n" +
                "playerID=" + playerID + "\n" +
                "alias=" + alias + "\n" +
                "bundleID=" + bundleID);
        }

        [MonoPInvokeCallback(typeof(GameCenterSignature.OnFailed))]
        private static void OnFailed(string reason)
        {
            Debug.Log("Failed to authenticate with gamecenter:" + reason);
        }

        private void OnLocalAuthenticateResult(bool success)
        {
            if (success)
            {
                Debug.Log("LocalAuthenticate success!");

                GameCenterSignature.Generate(OnSucceeded, OnFailed);
            }
            else
            {
                Debug.Log("LocalAuthentificate failed.");
            }
        }

        public void Process()
        {
            if (Social.localUser.authenticated)
            {
                GameCenterSignature.Generate(OnSucceeded, OnFailed);
            }
            else
            {
                Social.localUser.Authenticate(OnLocalAuthenticateResult);
            }
        }
    }
    #endif
}
```
