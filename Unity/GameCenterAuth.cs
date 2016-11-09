using UnityEngine;
using System.Collections;
using System.Runtime.InteropServices;

namespace Online
{
    public class GameCenterSignature 
    {
        public delegate void OnSucceeded ( 
            string PublicKeyUrl, 
            ulong timestamp,
            string signature,
            string salt,
            string playerID,
            string bundleID
        );

        public delegate void OnFailed ( 
            string Reason
        );

        #if (UNITY_IOS)
        [DllImport("__Internal")]    
        private static extern void GenerateIdentityVerificationSignature(OnSucceeded OnSucceeded, OnFailed OnFailed); 
        #endif

        public static void Generate(OnSucceeded OnSucceeded, OnFailed OnFailed)
        {
        #if (UNITY_IOS)
            GenerateIdentityVerificationSignature(OnSucceeded, OnFailed);
        #else
            OnFailed.Invoke("GameCenter authentication is only available for iOS");
        #endif
        }
    }
}
