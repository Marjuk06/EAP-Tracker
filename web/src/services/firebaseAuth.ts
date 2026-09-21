import { initializeApp, getApps, getApp } from 'firebase/app';
import {
  getAuth,
  signInWithPopup,
  GoogleAuthProvider,
  signOut,
  onAuthStateChanged,
  User
} from 'firebase/auth';

const firebaseConfig = {
  apiKey: "AIzaSyA8oFjcx0kMKNtjHJ0YeSJbokzzQxQY2wk",
  authDomain: "eap-tracker.firebaseapp.com",
  databaseURL: "https://eap-tracker-default-rtdb.firebaseio.com",
  projectId: "eap-tracker",
  storageBucket: "eap-tracker.firebasestorage.app",
  messagingSenderId: "786004356581",
  appId: "1:786004356581:web:eapadminmaster"
};

const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApp();
export const auth = getAuth(app);
const googleProvider = new GoogleAuthProvider();
googleProvider.setCustomParameters({ prompt: 'select_account' });

const STORAGE_WHITELIST_KEY = 'eap_admin_authorized_emails_v1';

export interface AdminGoogleProfile {
  uid: string;
  email: string | null;
  displayName: string | null;
  photoURL: string | null;
}

export const firebaseAuthService = {
  /**
   * Get list of authorized admin emails
   */
  getAuthorizedEmails(): string[] {
    const raw = localStorage.getItem(STORAGE_WHITELIST_KEY);
    if (!raw) return [];
    try {
      return JSON.parse(raw);
    } catch {
      return [];
    }
  },

  /**
   * Check if a given email is permitted to access the admin portal
   */
  isEmailAuthorized(email: string | null): boolean {
    if (!email) return false;
    const list = this.getAuthorizedEmails();
    // If no whitelist is set yet, allow the first Google sign-in to claim superadmin
    if (list.length === 0) return true;
    return list.some((e) => e.trim().toLowerCase() === email.trim().toLowerCase());
  },

  /**
   * Add a new email to the admin whitelist
   */
  addAuthorizedEmail(email: string): void {
    const clean = email.trim().toLowerCase();
    if (!clean) return;
    const current = this.getAuthorizedEmails();
    if (!current.includes(clean)) {
      current.push(clean);
      localStorage.setItem(STORAGE_WHITELIST_KEY, JSON.stringify(current));
    }
  },

  /**
   * Remove an email from the whitelist
   */
  removeAuthorizedEmail(email: string): void {
    const clean = email.trim().toLowerCase();
    const filtered = this.getAuthorizedEmails().filter((e) => e.toLowerCase() !== clean);
    localStorage.setItem(STORAGE_WHITELIST_KEY, JSON.stringify(filtered));
  },

  /**
   * Perform Google Sign-In with strict Admin Whitelist verification
   */
  async signInWithGoogle(): Promise<{ success: boolean; user?: AdminGoogleProfile; error?: string }> {
    try {
      const result = await signInWithPopup(auth, googleProvider);
      const user = result.user;
      const email = user.email;

      // Whitelist check
      const whitelist = this.getAuthorizedEmails();
      if (whitelist.length === 0 && email) {
        // First login claims superadmin ownership
        this.addAuthorizedEmail(email);
      } else if (!this.isEmailAuthorized(email)) {
        await signOut(auth);
        return {
          success: false,
          error: `Access Denied: "${email}" is not on the Authorized Admin list.`
        };
      }

      const profile: AdminGoogleProfile = {
        uid: user.uid,
        email: user.email,
        displayName: user.displayName,
        photoURL: user.photoURL
      };

      // Save session in sessionStorage
      sessionStorage.setItem('eap_admin_session_active', 'true');
      sessionStorage.setItem('eap_admin_google_user', JSON.stringify(profile));
      sessionStorage.setItem('eap_admin_session_last_active', Date.now().toString());

      return { success: true, user: profile };
    } catch (error: any) {
      console.error('Google Sign In Error:', error);
      let msg = error.message || 'Google sign in failed.';
      if (error.code === 'auth/popup-closed-by-user') {
        msg = 'Google Sign-In popup was closed.';
      } else if (error.code === 'auth/cancelled-popup-request') {
        msg = 'Sign-in request was cancelled.';
      } else if (error.code === 'auth/network-request-failed') {
        msg = 'Network connection failed.';
      }
      return { success: false, error: msg };
    }
  },

  async signOutAdmin(): Promise<void> {
    try {
      await signOut(auth);
    } catch (e) {
      console.warn('Sign out error:', e);
    }
    sessionStorage.removeItem('eap_admin_session_active');
    sessionStorage.removeItem('eap_admin_google_user');
    sessionStorage.removeItem('eap_admin_session_last_active');
  },

  getCurrentGoogleAdmin(): AdminGoogleProfile | null {
    const raw = sessionStorage.getItem('eap_admin_google_user');
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  },

  onAuthStateChange(callback: (user: AdminGoogleProfile | null) => void) {
    return onAuthStateChanged(auth, (firebaseUser: User | null) => {
      if (firebaseUser) {
        if (!this.isEmailAuthorized(firebaseUser.email)) {
          this.signOutAdmin();
          callback(null);
          return;
        }

        const profile: AdminGoogleProfile = {
          uid: firebaseUser.uid,
          email: firebaseUser.email,
          displayName: firebaseUser.displayName,
          photoURL: firebaseUser.photoURL
        };
        sessionStorage.setItem('eap_admin_session_active', 'true');
        sessionStorage.setItem('eap_admin_google_user', JSON.stringify(profile));
        callback(profile);
      } else {
        const localGoogle = this.getCurrentGoogleAdmin();
        if (!localGoogle) {
          callback(null);
        }
      }
    });
  }
};
