import { firebaseAuthService, AdminGoogleProfile } from './firebaseAuth';

declare global {
  interface Window {
    google?: any;
  }
}

const CLIENT_ID = "786004356581-22mkt3lbf0rngpns3dagvknh83dpkk9v.apps.googleusercontent.com";
const SCOPES = "https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile";

const STORAGE_ACCESS_TOKEN = "eap_gdrive_access_token";
const STORAGE_LAST_BACKUP_TIME = "eap_gdrive_last_backup_time";
const STORAGE_LAST_BACKUP_SIZE = "eap_gdrive_last_backup_size";

export interface GoogleDriveBackupItem {
  id: string;
  name: string;
  createdTime: string;
  size: string;
}

export const googleDriveService = {
  getAccessToken(): string | null {
    return sessionStorage.getItem(STORAGE_ACCESS_TOKEN) || localStorage.getItem(STORAGE_ACCESS_TOKEN);
  },

  setAccessToken(token: string) {
    sessionStorage.setItem(STORAGE_ACCESS_TOKEN, token);
    localStorage.setItem(STORAGE_ACCESS_TOKEN, token);
  },

  clearAccessToken() {
    sessionStorage.removeItem(STORAGE_ACCESS_TOKEN);
    localStorage.removeItem(STORAGE_ACCESS_TOKEN);
  },

  getLastBackupTime(): string {
    return localStorage.getItem(STORAGE_LAST_BACKUP_TIME) || 'Never';
  },

  getLastBackupSize(): string {
    return localStorage.getItem(STORAGE_LAST_BACKUP_SIZE) || '0 KB';
  },

  /**
   * 1-Click Sign-in using Google Identity Services (GSI) with Drive scopes
   */
  async signInWithGoogle(): Promise<{ success: boolean; user?: AdminGoogleProfile; error?: string }> {
    return new Promise((resolve) => {
      // Check if Google SDK is loaded
      if (window.google?.accounts?.oauth2) {
        try {
          const client = window.google.accounts.oauth2.initTokenClient({
            client_id: CLIENT_ID,
            scope: SCOPES,
            callback: async (response: any) => {
              if (response.error) {
                console.error("Google Auth error:", response);
                resolve({ success: false, error: response.error_description || response.error });
                return;
              }

              const accessToken = response.access_token;
              this.setAccessToken(accessToken);

              // Fetch User Profile with Token
              try {
                const userRes = await fetch("https://www.googleapis.com/oauth2/v3/userinfo", {
                  headers: { Authorization: `Bearer ${accessToken}` }
                });
                if (!userRes.ok) {
                  throw new Error(`Failed to fetch user profile: HTTP ${userRes.status}`);
                }
                const userData = await userRes.json();
                const email = userData.email || null;

                // Whitelist check
                if (!firebaseAuthService.isEmailAuthorized(email)) {
                  this.clearAccessToken();
                  resolve({
                    success: false,
                    error: `Access Denied: "${email}" is not authorized as an Admin.`
                  });
                  return;
                }

                const profile: AdminGoogleProfile = {
                  uid: userData.sub || `google-${Date.now()}`,
                  email: email,
                  displayName: userData.name || userData.given_name || 'Admin',
                  photoURL: userData.picture || null
                };

                sessionStorage.setItem('eap_admin_session_active', 'true');
                sessionStorage.setItem('eap_admin_google_user', JSON.stringify(profile));
                sessionStorage.setItem('eap_admin_session_last_active', Date.now().toString());

                resolve({ success: true, user: profile });
              } catch (e: any) {
                resolve({ success: false, error: e.message });
              }
            }
          });
          client.requestAccessToken({ prompt: 'select_account' });
        } catch (e: any) {
          console.warn("GSI initTokenClient fallback to Firebase Auth:", e);
          // Fallback to Firebase Auth
          firebaseAuthService.signInWithGoogle().then(resolve);
        }
      } else {
        // Fallback to Firebase Auth
        firebaseAuthService.signInWithGoogle().then(resolve);
      }
    });
  },

  /**
   * Upload Master Backup JSON to Google Drive
   */
  async uploadBackupToGoogleDrive(payload: any): Promise<{ success: boolean; fileId?: string; error?: string }> {
    let token = this.getAccessToken();
    if (!token) {
      const loginRes = await this.signInWithGoogle();
      if (!loginRes.success) {
        return { success: false, error: loginRes.error || "Please sign in to Google Drive first." };
      }
      token = this.getAccessToken();
    }

    if (!token) {
      return { success: false, error: "Google Drive Access Token missing. Please sign in." };
    }

    try {
      const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
      const filename = `EAP_Tracker_Master_Backup_${timestamp}.json`;
      const jsonContent = JSON.stringify(payload, null, 2);

      const metadata = {
        name: filename,
        mimeType: 'application/json',
        description: 'EAP Tracker Master Admin Full Backup Bundle'
      };

      const boundary = '-------314159265358979323846';
      const delimiter = `\r\n--${boundary}\r\n`;
      const closeDelimiter = `\r\n--${boundary}--`;

      const multipartRequestBody =
        delimiter +
        'Content-Type: application/json; charset=UTF-8\r\n\r\n' +
        JSON.stringify(metadata) +
        delimiter +
        'Content-Type: application/json\r\n\r\n' +
        jsonContent +
        closeDelimiter;

      const res = await fetch('https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': `multipart/related; boundary=${boundary}`
        },
        body: multipartRequestBody
      });

      if (!res.ok) {
        if (res.status === 401) {
          this.clearAccessToken();
          return { success: false, error: "Google Drive session expired. Please sign in again." };
        }
        const errJson = await res.json().catch(() => ({}));
        throw new Error(errJson.error?.message || `HTTP ${res.status}`);
      }

      const fileData = await res.json();

      // Record backup time and size
      const dateStr = new Date().toLocaleString();
      const sizeKB = (new Blob([jsonContent]).size / 1024).toFixed(1) + ' KB';
      localStorage.setItem(STORAGE_LAST_BACKUP_TIME, dateStr);
      localStorage.setItem(STORAGE_LAST_BACKUP_SIZE, sizeKB);

      return { success: true, fileId: fileData.id };
    } catch (err: any) {
      console.error("Google Drive Upload Error:", err);
      return { success: false, error: err.message || "Failed to upload to Google Drive" };
    }
  },

  /**
   * List backups from Google Drive
   */
  async listGoogleDriveBackups(): Promise<{ success: boolean; backups: GoogleDriveBackupItem[]; error?: string }> {
    let token = this.getAccessToken();
    if (!token) {
      return { success: false, backups: [], error: "Not signed in to Google Drive." };
    }

    try {
      const q = encodeURIComponent("name contains 'EAP_Tracker_Master_Backup' and trashed=false");
      const url = `https://www.googleapis.com/drive/v3/files?q=${q}&fields=files(id,name,createdTime,size)&orderBy=createdTime desc&pageSize=15`;
      
      const res = await fetch(url, {
        headers: { Authorization: `Bearer ${token}` }
      });

      if (!res.ok) {
        if (res.status === 401) {
          this.clearAccessToken();
          return { success: false, backups: [], error: "Session expired." };
        }
        throw new Error(`HTTP ${res.status}`);
      }

      const data = await res.json();
      const items: GoogleDriveBackupItem[] = (data.files || []).map((f: any) => ({
        id: f.id,
        name: f.name,
        createdTime: f.createdTime ? new Date(f.createdTime).toLocaleString() : 'Unknown',
        size: f.size ? (parseInt(f.size, 10) / 1024).toFixed(1) + ' KB' : 'Unknown'
      }));

      return { success: true, backups: items };
    } catch (err: any) {
      return { success: false, backups: [], error: err.message };
    }
  },

  /**
   * Download and Restore a backup from Google Drive
   */
  async downloadBackupFromGoogleDrive(fileId: string): Promise<{ success: boolean; data?: any; error?: string }> {
    let token = this.getAccessToken();
    if (!token) {
      return { success: false, error: "Not signed in to Google Drive." };
    }

    try {
      const url = `https://www.googleapis.com/drive/v3/files/${fileId}?alt=media`;
      const res = await fetch(url, {
        headers: { Authorization: `Bearer ${token}` }
      });

      if (!res.ok) {
        throw new Error(`Download failed: HTTP ${res.status}`);
      }

      const data = await res.json();
      return { success: true, data };
    } catch (err: any) {
      return { success: false, error: err.message };
    }
  }
};
