# OWASP Security Implementation

## M4: Insufficient Input/Output Validation

1. **Enhanced Song Repository Validation**

   - Make sure add validation for song title, artist, and file paths (not blank and not reach 100 characters, the existence of the song)
   - Implemented URI format verification (not blank, valid URI)
   - Established duration limits to prevent resource exhaustion (30 minutes for audio, non negative duration)

2. **Improved Login Validation**

   - Added proper email format validation (not blank, valid email, not reach 100 characters)
   - Implemented password length and complexity checks (not blank, between 6 and 100 characters)
   - Sanitized error messages to prevent information leakage (not expose errors details about 401, connection, and timeout)
   - Added client-side validation before network requests

## M8: Security Misconfiguration

1. **Network Security Configuration**

   - Created a network security configuration file to restrict cleartext traffic (add interceptor for auth guards, read timeout, connect timeout, prohibited follow redirects, doing retry connection failure )
   - Limited permitted connections to specific domains (only allow connections to trusted domains)

2. **Secure OkHttpClient Configuration**

   - Enforced modern TLS versions (TLS 1.2 and 1.3)
   - Added proper timeouts to prevent DoS vulnerabilities
   - Implemented proper SSL socket factory configuration
   - Added security headers to API requests

## M9: Insecure Data Storage

1. **Encrypted Token Storage**

   - Implemented Android Keystore for secure key management (production needs)
   - Added AES-GCM encryption for token storage
   - Created proper IV handling for each encryption operation
   - Implemented secure token deletion
