export interface Env {
  FCM_PROJECT_ID: string;
  FCM_CLIENT_EMAIL: string;
  FCM_PRIVATE_KEY: string;
  ADMIN_SECRET: string;
}

// Convert base64url to Uint8Array
function b64url2u8(str: string): Uint8Array {
  str = str.replace(/-/g, '+').replace(/_/g, '/');
  const padding = str.length % 4;
  if (padding) {
    str += '='.repeat(4 - padding);
  }
  const binary = atob(str);
  const u8 = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    u8[i] = binary.charCodeAt(i);
  }
  return u8;
}

// Convert Uint8Array to base64url
function u82b64url(u8: Uint8Array): string {
  let binary = '';
  for (let i = 0; i < u8.length; i++) {
    binary += String.fromCharCode(u8[i]);
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

// Import PEM private key into CryptoKey
async function importPrivateKey(pem: string): Promise<CryptoKey> {
  const pemHeader = '-----BEGIN PRIVATE KEY-----';
  const pemFooter = '-----END PRIVATE KEY-----';
  const pemContents = pem
    .replace(pemHeader, '')
    .replace(pemFooter, '')
    .replace(/\s/g, ''); // Remove all whitespace

  const binaryDer = b64url2u8(pemContents);
  
  return await crypto.subtle.importKey(
    'pkcs8',
    binaryDer.buffer,
    {
      name: 'RSASSA-PKCS1-v1_5',
      hash: 'SHA-256',
    },
    false,
    ['sign']
  );
}

// Sign JWT using RSASSA-PKCS1-v1_5
async function signJwt(header: object, payload: object, privateKey: CryptoKey): Promise<string> {
  const headerB64 = u82b64url(new TextEncoder().encode(JSON.stringify(header)));
  const payloadB64 = u82b64url(new TextEncoder().encode(JSON.stringify(payload)));
  const dataToSign = `${headerB64}.${payloadB64}`;

  const signature = await crypto.subtle.sign(
    'RSASSA-PKCS1-v1_5',
    privateKey,
    new TextEncoder().encode(dataToSign)
  );

  const signatureB64 = u82b64url(new Uint8Array(signature));
  return `${dataToSign}.${signatureB64}`;
}

// Get Google OAuth 2.0 Access Token
async function getGoogleAccessToken(clientEmail: string, privateKeyPem: string): Promise<string> {
  const iat = Math.floor(Date.now() / 1000);
  const exp = iat + 3600; // 1 hour

  const header = {
    alg: 'RS256',
    typ: 'JWT',
  };

  const payload = {
    iss: clientEmail,
    scope: 'https://www.googleapis.com/auth/firebase.messaging',
    aud: 'https://oauth2.googleapis.com/token',
    exp: exp,
    iat: iat,
  };

  const privateKey = await importPrivateKey(privateKeyPem);
  const jwt = await signJwt(header, payload, privateKey);

  const response = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
  });

  const data = await response.json() as { access_token: string, error?: string, error_description?: string };
  if (!response.ok || data.error) {
    throw new Error(`Failed to get OAuth token: ${data.error_description || data.error || response.statusText}`);
  }

  return data.access_token;
}

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Admin-Key',
};

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    // 1. Handle CORS Preflight
    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    // 2. Enforce POST only
    if (request.method !== 'POST') {
      return new Response('Method not allowed', { status: 405, headers: corsHeaders });
    }

    // 3. Authenticate the Admin Panel
    // Require the Authorization header to match the ADMIN_SECRET configured in Cloudflare
    const authHeader = request.headers.get('Authorization') || request.headers.get('X-Admin-Key');
    const expectedAuth = `Bearer ${env.ADMIN_SECRET}`;
    
    if (!env.ADMIN_SECRET) {
      return new Response(JSON.stringify({ error: 'Worker ADMIN_SECRET not configured' }), { 
        status: 500, 
        headers: { 'Content-Type': 'application/json', ...corsHeaders } 
      });
    }

    if (!authHeader || authHeader !== expectedAuth) {
      return new Response(JSON.stringify({ error: 'Unauthorized: Invalid Admin Secret' }), { 
        status: 401, 
        headers: { 'Content-Type': 'application/json', ...corsHeaders } 
      });
    }

    try {
      // 4. Parse incoming notification payload from Admin Panel
      const body = await request.json() as any;
      const { topic, token, title, message, data } = body;

      if ((!topic && !token) || (topic && token) || !title) {
        return new Response(JSON.stringify({ error: 'Must provide exactly one of "topic" or "token", along with "title"' }), { 
          status: 400, 
          headers: { 'Content-Type': 'application/json', ...corsHeaders } 
        });
      }

      // 5. Get OAuth Token for FCM HTTP v1 API
      // Private key must be stored in CF Secrets with actual line breaks or `\n` characters correctly handled
      const privateKey = env.FCM_PRIVATE_KEY.replace(/\\n/g, '\n');
      const accessToken = await getGoogleAccessToken(env.FCM_CLIENT_EMAIL, privateKey);

      // 6. Construct FCM HTTP v1 Payload supporting both Topic and Individual Token targeting
      const targetConfig: any = {};
      if (token) {
        targetConfig.token = token;
      } else {
        targetConfig.topic = topic;
      }

      const fcmPayload = {
        message: {
          ...targetConfig,
          notification: {
            title: title,
            body: message || ''
          },
          data: data || {},
          android: {
            priority: 'high',
            notification: {
              sound: 'default',
              channel_id: 'eap_schedule_updates' // Must match Android channel config
            }
          }
        }
      };

      // 7. Send the push request
      const fcmUrl = `https://fcm.googleapis.com/v1/projects/${env.FCM_PROJECT_ID}/messages:send`;
      const fcmResponse = await fetch(fcmUrl, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(fcmPayload)
      });

      const fcmResult = await fcmResponse.json() as any;

      if (!fcmResponse.ok) {
        return new Response(JSON.stringify({ error: 'FCM API Error', details: fcmResult }), { 
          status: 502, 
          headers: { 'Content-Type': 'application/json', ...corsHeaders } 
        });
      }

      // 8. Return success
      return new Response(JSON.stringify({ success: true, name: fcmResult.name }), {
        status: 200,
        headers: { 'Content-Type': 'application/json', ...corsHeaders }
      });

    } catch (error: any) {
      return new Response(JSON.stringify({ error: error.message }), {
        status: 500,
        headers: { 'Content-Type': 'application/json', ...corsHeaders }
      });
    }
  },
};
