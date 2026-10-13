#include <CoreFoundation/CoreFoundation.h>
#include <Security/Security.h>

/* macOS 12's copy API expressed using the certificate-chain APIs in Catalina. */
CFArrayRef SecTrustCopyCertificateChain(SecTrustRef trust) {
    if (trust == NULL) return NULL;
    CFIndex count = SecTrustGetCertificateCount(trust);
    CFMutableArrayRef chain = CFArrayCreateMutable(kCFAllocatorDefault, count,
                                                  &kCFTypeArrayCallBacks);
    if (chain == NULL) return NULL;
    for (CFIndex index = 0; index < count; index++) {
        SecCertificateRef certificate = SecTrustGetCertificateAtIndex(trust, index);
        if (certificate != NULL) CFArrayAppendValue(chain, certificate);
    }
    return chain;
}
