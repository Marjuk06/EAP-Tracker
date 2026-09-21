import React, { useState, useEffect, useRef } from 'react';
import {
 Shield, Key, Lock, Unlock, Plus, Edit2, Trash2, RotateCcw,
 CloudUpload, CloudDownload, Copy, Check, Calendar, FileText,
 BookOpen, ChevronRight, X, Sparkles, RefreshCw, Layers,
 Download, Upload, FileUp, Database, Quote, Send, Flame,
 Target, Compass, Heart, Award, Zap, TrendingUp, Bell, Volume2,
 MessageSquare, Users, UserCheck, Smartphone, Activity, CheckCircle2,
 Search, ExternalLink, User, Clock, Code2, Sun, Moon,
 AlertTriangle, AlertCircle, Radio, Brain, Link, Globe, Pin, Brush, Star,
 GripVertical, ArrowUpDown, ArrowLeft, FileCode, Rocket, ArrowUp, ArrowDown,
 CheckSquare, Square, Edit3, ToggleLeft, ToggleRight, ListOrdered,
 HelpCircle, BookMarked, Lightbulb, Info, CheckCircle, LifeBuoy
} from 'lucide-react';
import { RoutineItem, ExamRecord, Subject } from '../types';
import { initialRoutines, initialSubjects, admissionExamPresets } from '../data/initialData';
import { adminQuotesLibrary, MotivationQuoteItem } from '../data/quotesData';
import masterEapData from '../data/master_eap_data.json';
import { sound } from '../services/audio';
import { adminAuth } from '../services/adminAuth';
import { firebaseAuthService, AdminGoogleProfile } from '../services/firebaseAuth';
import { googleDriveService, GoogleDriveBackupItem } from '../services/googleDriveService';

export interface StudentTelemetry {
 studentId: string;
 name: string;
 college: string;
 batch: string;
 targetInstitution: string;
 email: string;
 photoUrl: string;
 dailyGoalHours: number;
 todayStudyHours: number;
 completedChapters: number;
 totalChapters: number;
 syllabusPct: number;
 totalExamsLogged: number;
 examAveragePct: number;
 deviceModel: string;
 lastActiveEpoch: number;
 isStudying?: boolean;
 activeSubject?: string;
 sessionStartEpoch?: number;
 preferredTrack?: string;
 appVersion?: string;
}

export interface PresetTemplate {
 id: string;
 name: string;
 category: 'Syllabus' | 'Routine' | 'Exams' | 'Bundle' | 'FullTrack';
 description: string;
 createdAt: number;
 data: {
  subjects?: Subject[];
  routineDays?: RoutineItem[];
  routineMode?: 'Offline' | 'Online';
  exams?: any[];
 };
}

export type AdminTabId = 'routines' | 'exams' | 'quotes' | 'syllabus' | 'presets' | 'cloud' | 'students' | 'notifications' | 'help';

interface AdminScreenProps {
 onBack?: () => void;
 onOpenStudentPreview?: () => void;
 onInstallApp?: () => void;
 canInstallApp?: boolean;
 initialTab?: AdminTabId;
 onTabChange?: (tab: AdminTabId) => void;
}

export const AdminScreen: React.FC<AdminScreenProps> = ({
  onBack,
  onOpenStudentPreview,
  onInstallApp,
  canInstallApp,
  initialTab,
  onTabChange
}) => {
 // Authentication State via enterprise adminAuth & Google Sign-In
 const [googleAdminUser, setGoogleAdminUser] = useState<AdminGoogleProfile | null>(() => firebaseAuthService.getCurrentGoogleAdmin());
 const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => adminAuth.isAuthenticated() || !!firebaseAuthService.getCurrentGoogleAdmin());
 const [passwordInput, setPasswordInput] = useState<string>('');
 const [authError, setAuthError] = useState<string>('');
 const [authLoading, setAuthLoading] = useState<boolean>(false);
 const [lockoutSec, setLockoutSec] = useState<number>(() => adminAuth.getLockoutRemainingSeconds());

 useEffect(() => {
   const unsub = firebaseAuthService.onAuthStateChange((user) => {
     if (user) {
       setGoogleAdminUser(user);
       setIsAuthenticated(true);
     }
   });
   return () => unsub();
 }, []);

  // Change Password & Security Modal State
  const [isChangePasswordModalOpen, setIsChangePasswordModalOpen] = useState<boolean>(false);
  const [currentPasswordInput, setCurrentPasswordInput] = useState<string>('');
  const [newPasswordInput, setNewPasswordInput] = useState<string>('');
  const [confirmPasswordInput, setConfirmPasswordInput] = useState<string>('');
  const [changePasswordError, setChangePasswordError] = useState<string>('');
  const [authorizedEmailsList, setAuthorizedEmailsList] = useState<string[]>(() => firebaseAuthService.getAuthorizedEmails());
  const [newAdminEmailInput, setNewAdminEmailInput] = useState<string>('');

  // Google Drive Cloud Backup State
  const [gdriveBackupsList, setGdriveBackupsList] = useState<GoogleDriveBackupItem[]>([]);
  const [isGdriveBackingUp, setIsGdriveBackingUp] = useState<boolean>(false);
  const [isGdriveLoading, setIsGdriveLoading] = useState<boolean>(false);
  const [gdriveLastTime, setGdriveLastTime] = useState<string>(() => googleDriveService.getLastBackupTime());
  const [gdriveLastSize, setGdriveLastSize] = useState<string>(() => googleDriveService.getLastBackupSize());
  const [isRestoreModalOpen, setIsRestoreModalOpen] = useState<boolean>(false);

 // Activity-based session keepalive and lockout countdown
 useEffect(() => {
   if (lockoutSec <= 0) return;
   const interval = setInterval(() => {
     const rem = adminAuth.getLockoutRemainingSeconds();
     setLockoutSec(rem);
     if (rem <= 0) {
       clearInterval(interval);
       setAuthError('');
     }
   }, 1000);
   return () => clearInterval(interval);
 }, [lockoutSec]);

 useEffect(() => {
   const onActivity = () => adminAuth.touchActivity();
   window.addEventListener('click', onActivity);
   window.addEventListener('keydown', onActivity);
   return () => {
     window.removeEventListener('click', onActivity);
     window.removeEventListener('keydown', onActivity);
   };
 }, []);

 // Active Tab: 9 Connected Navigation Tabs
 const [activeTab, setActiveTab] = useState<AdminTabId>(initialTab || 'routines');
 useEffect(() => {
  if (initialTab && initialTab !== activeTab) {
   setActiveTab(initialTab);
  }
 }, [initialTab]);

 const setActiveTabWithRoute = (tab: AdminTabId) => {
  setActiveTab(tab);
  onTabChange?.(tab);
 };
 const [routineMode, setRoutineMode] = useState<'Offline' | 'Online'>('Offline');
 const [examMode, setExamMode] = useState<'Offline' | 'Online'>('Offline');

 const fileInputRef = useRef<HTMLInputElement>(null);
 const routineJsonUploadRef = useRef<HTMLInputElement>(null);
 const examJsonUploadRef = useRef<HTMLInputElement>(null);
 const [isExportHubModalOpen, setIsExportHubModalOpen] = useState<boolean>(false);

 // Student Telemetry State
 const [studentsList, setStudentsList] = useState<StudentTelemetry[]>([]);
 const [isLoadingStudents, setIsLoadingStudents] = useState<boolean>(false);
 const [studentSearchQuery, setStudentSearchQuery] = useState<string>('');
 const [detectedGhostCount, setDetectedGhostCount] = useState<number>(0);

 // Multi-Student Roster Selection & Mass Actions
 const [selectedStudentRosterIds, setSelectedStudentRosterIds] = useState<string[]>([]);
 const [isMassAssignModalOpen, setIsMassAssignModalOpen] = useState<boolean>(false);
 const [selectedPresetForMassAssign, setSelectedPresetForMassAssign] = useState<PresetTemplate | null>(null);

 // Default Built-in Preset Library
 const defaultBuiltInPresets: PresetTemplate[] = [
  {
   id: 'preset_buet_pure_engg',
   name: 'BUET Pure Engineering Track (No Bio)',
   category: 'Syllabus',
   description: 'Physics 1st & 2nd, Chemistry 1st & 2nd, Higher Math 1st & 2nd. Biology excluded for pure engineering focus.',
   createdAt: 1700000000000,
   data: {
    subjects: initialSubjects.filter(s => !s.name.toLowerCase().includes('bio'))
   }
  },
  {
   id: 'preset_medical_track',
   name: 'Medical Admission Track (Complete)',
   category: 'Syllabus',
   description: 'Biology, Chemistry, Physics, Medical English Grammar & General Knowledge curriculum for medical aspirants.',
   createdAt: 1700000000000,
   data: {
    subjects: [
     ...initialSubjects.filter(s => s.name.toLowerCase().includes('bio') || s.name.toLowerCase().includes('chem') || s.name.toLowerCase().includes('phys')),
     {
      id: 'sub_english_med',
      name: 'English for Medical',
      iconName: 'BookOpen',
      papers: [
       {
        id: 'pap_eng_1',
        name: 'English Grammar & Vocabulary',
        chapters: [
         { id: 'ch_eng_1', name: 'Parts of Speech & Identification', sections: [] },
         { id: 'ch_eng_2', name: 'Tense & Right Form of Verbs', sections: [] },
         { id: 'ch_eng_3', name: 'Subject-Verb Agreement', sections: [] },
         { id: 'ch_eng_4', name: 'Prepositions & Idioms', sections: [] },
         { id: 'ch_eng_5', name: 'Synonyms & Antonyms', sections: [] }
        ]
       }
      ]
     },
     {
      id: 'sub_gk_med',
      name: 'General Knowledge (Medical)',
      iconName: 'BookOpen',
      papers: [
       {
        id: 'pap_gk_1',
        name: 'Bangladesh & World Affairs',
        chapters: [
         { id: 'ch_gk_1', name: 'Liberation War & History of Bangladesh', sections: [] },
         { id: 'ch_gk_2', name: 'Constitution & Government', sections: [] },
         { id: 'ch_gk_3', name: 'Important Inventions & Discoveries', sections: [] }
        ]
       }
      ]
     }
    ]
   }
  },
  {
   id: 'preset_du_ka_track',
   name: "Dhaka University 'KA' Unit Track",
   category: 'Syllabus',
   description: 'Physics, Chemistry, Higher Math, Biology and HSC ICT.',
   createdAt: 1700000000000,
   data: {
    subjects: [
     ...initialSubjects,
     {
      id: 'sub_ict_du',
      name: 'ICT (Information & Communication Tech)',
      iconName: 'BookOpen',
      papers: [
       {
        id: 'pap_ict_1',
        name: 'HSC ICT Curriculum',
        chapters: [
         { id: 'ch_ict_1', name: 'বিশ্ব ও বাংলাদেশ প্রেক্ষিত', sections: [] },
         { id: 'ch_ict_2', name: 'কমিউনিকেশন সিস্টেমস ও নেটওয়ার্কিং', sections: [] },
         { id: 'ch_ict_3', name: 'সংখ্যা পদ্ধতি ও ডিজিটাল ডিভাইস', sections: [] },
         { id: 'ch_ict_4', name: 'ওয়েব ডিজাইন ও HTML', sections: [] },
         { id: 'ch_ict_5', name: 'প্রোগ্রামিং ভাষা (C Programming)', sections: [] },
         { id: 'ch_ict_6', name: 'ডাটাবেজ ম্যানেজমেন্ট সিস্টেম', sections: [] }
        ]
       }
      ]
     }
    ]
   }
  },
  {
   id: 'preset_crash_30_days',
   name: '30-Day Intensive Crash Routine',
   category: 'Routine',
   description: 'Curated 30-day intensive revision schedule with daily high-yield tests.',
   createdAt: 1700000000000,
   data: {
    routineDays: initialRoutines.slice(0, 30),
    routineMode: 'Offline'
   }
  },
  {
   id: 'preset_ckruet_special',
   name: 'CKRUET Engineering Sprint Track',
   category: 'Routine',
   description: 'RUET, KUET, CUET combined admission preparation crash routine & weekly exams.',
   createdAt: 1700000000000,
   data: {
    routineDays: initialRoutines.slice(0, 45),
    routineMode: 'Offline'
   }
  }
 ];

 // Presets & Track Library State
 const [presetsList, setPresetsList] = useState<PresetTemplate[]>(() => {
  try {
   const saved = localStorage.getItem('eap_admin_presets');
   if (saved) return JSON.parse(saved);
  } catch (_e) {}
  return defaultBuiltInPresets;
 });

 // Presets & Track Studio Dedicated States
 const [presetFilterCategory, setPresetFilterCategory] = useState<'All' | 'Syllabus' | 'Routine' | 'Exams' | 'Bundle'>('All');
 const [presetSearchQuery, setPresetSearchQuery] = useState<string>('');
 const [editingPreset, setEditingPreset] = useState<PresetTemplate | null>(null);
 const [isCreatingNewPreset, setIsCreatingNewPreset] = useState<boolean>(false);
 const [presetEditorTab, setPresetEditorTab] = useState<'visual' | 'json'>('visual');
 const [presetRawJsonText, setPresetRawJsonText] = useState<string>('');
 const presetJsonUploadRef = useRef<HTMLInputElement>(null);

 // Admin Master Guide & Help Center Dedicated States
 const [helpSearchQuery, setHelpSearchQuery] = useState<string>('');
 const [helpSelectedCategory, setHelpSelectedCategory] = useState<string>('all');

 // Universal Save Preset Handler from ANY Screen
 const handleSavePresetFromData = (name: string, category: 'Syllabus' | 'Routine' | 'Exams' | 'Bundle', data: any, description?: string) => {
  const newPreset: PresetTemplate = {
   id: `preset_${Date.now()}`,
   name,
   category,
   description: description || `Custom ${category.toLowerCase()} track created by mentor`,
   createdAt: Date.now(),
   data
  };
  setPresetsList((prev) => {
   const updated = [newPreset, ...prev];
   try {
    localStorage.setItem('eap_admin_presets', JSON.stringify(updated));
   } catch (_e) {}
   return updated;
  });
  sound.playSuccess();
  showNotification(`Track preset "${name}" saved to Presets Studio!`);
 };

 // Upload & Import JSON File as Preset Track
 const handleImportPresetJsonFile = (e: React.ChangeEvent<HTMLInputElement>) => {
  const file = e.target.files?.[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = (ev) => {
   try {
    const content = ev.target?.result as string;
    const parsed = JSON.parse(content);

    let category: 'Syllabus' | 'Routine' | 'Exams' | 'Bundle' = 'Routine';
    let data: any = {};
    let defaultName = file.name.replace(/\.[^/.]+$/, '').replace(/[_|-]/g, ' ');

    if (parsed.category && parsed.data) {
     category = parsed.category;
     data = parsed.data;
     defaultName = parsed.name || defaultName;
    } else if (Array.isArray(parsed)) {
     if (parsed[0] && (parsed[0].classSubject || parsed[0].topics)) {
      category = 'Routine';
      data = { routineDays: parsed, routineMode: 'Offline' };
     } else if (parsed[0] && (parsed[0].exams || parsed[0].syllabus)) {
      category = 'Exams';
      data = { exams: parsed };
     } else if (parsed[0] && parsed[0].papers) {
      category = 'Syllabus';
      data = { subjects: parsed };
     }
    } else if (parsed.subjects && Array.isArray(parsed.subjects)) {
     category = 'Syllabus';
     data = { subjects: parsed.subjects };
    } else if (parsed.offlineRoutine || parsed.onlineRoutine || parsed.routines) {
     category = 'Routine';
     data = { routineDays: parsed.offlineRoutine || parsed.onlineRoutine || parsed.routines, routineMode: 'Offline' };
    } else if (parsed.offlineExams || parsed.onlineExams || parsed.exams) {
     category = 'Exams';
     data = { exams: parsed.offlineExams || parsed.onlineExams || parsed.exams };
    }

    const newPreset: PresetTemplate = {
     id: `preset_${Date.now()}`,
     name: defaultName,
     category,
     description: `Imported from ${file.name}`,
     createdAt: Date.now(),
     data
    };

    setPresetsList((prev) => {
     const updated = [newPreset, ...prev];
     try {
      localStorage.setItem('eap_admin_presets', JSON.stringify(updated));
     } catch (_e) {}
     return updated;
    });

    sound.playSuccess();
    showNotification(`Imported preset track "${defaultName}" successfully!`);
   } catch (err: any) {
    sound.playNegative();
    showNotification(`Failed to parse preset JSON: ${err.message}`, 'error');
   }
  };
  reader.readAsText(file);
  e.target.value = '';
 };

 // Student Detail & Custom Routine / Exam Modal State
 const cleanStudentKey = (id: string) => (id || '').trim().replace(/[^a-zA-Z0-9_-]/g, '_');
 const [selectedStudentForDetail, setSelectedStudentForDetail] = useState<StudentTelemetry | null>(null);
 const [studentDetailSubTab, setStudentDetailSubTab] = useState<'offlineRoutine' | 'onlineRoutine' | 'offlineExams' | 'onlineExams' | 'customSyllabus'>('offlineRoutine');
 const [studentCustomRoutine, setStudentCustomRoutine] = useState<RoutineItem[] | null>(null);
 const [studentCustomOnlineRoutine, setStudentCustomOnlineRoutine] = useState<RoutineItem[] | null>(null);
 const [studentCustomOfflineExams, setStudentCustomOfflineExams] = useState<any[] | null>(null);
 const [studentCustomOnlineExams, setStudentCustomOnlineExams] = useState<any[] | null>(null);
 const [studentCustomSyllabus, setStudentCustomSyllabus] = useState<Subject[] | null>(null);
 const [isLoadingCustomRoutine, setIsLoadingCustomRoutine] = useState<boolean>(false);
 const [isLoadingCustomSyllabus, setIsLoadingCustomSyllabus] = useState<boolean>(false);
 const [editingDayIndex, setEditingDayIndex] = useState<number | null>(null);
 const [editingDayData, setEditingDayData] = useState<any | null>(null);
 const [editingSubjectIndex, setEditingSubjectIndex] = useState<number | null>(null);
 const [editingSubjectName, setEditingSubjectName] = useState<string>('');
 const [isShowDemoJsonModal, setIsShowDemoJsonModal] = useState<boolean>(false);
 const [isDirectJsonEditorOpen, setIsDirectJsonEditorOpen] = useState<boolean>(false);
 const [directJsonInputText, setDirectJsonInputText] = useState<string>('');

 // Multi-Select Batch Selection States across All Screens
 const [selectedRoutineIndices, setSelectedRoutineIndices] = useState<number[]>([]);
 const [selectedExamIndices, setSelectedExamIndices] = useState<number[]>([]);
 const [selectedSubjectIndices, setSelectedSubjectIndices] = useState<number[]>([]);
 const [selectedStudentRoutineIndices, setSelectedStudentRoutineIndices] = useState<number[]>([]);
 const [selectedStudentExamIndices, setSelectedStudentExamIndices] = useState<number[]>([]);
 const [selectedStudentSubjectIndices, setSelectedStudentSubjectIndices] = useState<number[]>([]);

 // In-App Auto-Update & Remote APK Release Manager State
 const [appUpdateVersionName, setAppUpdateVersionName] = useState<string>('v1.0.1');
 const [appUpdateVersionCode, setAppUpdateVersionCode] = useState<number>(2);
 const [appUpdateApkUrl, setAppUpdateApkUrl] = useState<string>('');
 const [appUpdateTitle, setAppUpdateTitle] = useState<string>(' New EAP Tracker Update Available');
 const [appUpdateChangelog, setAppUpdateChangelog] = useState<string>('• Real-time Routine & Exam Schedule Sync\n• High-priority Instant Notification Push\n• Performance optimizations and bug fixes');
 const [appUpdateIsForce, setAppUpdateIsForce] = useState<boolean>(false);

 // Targeted Test App Update State
 const [appUpdateTargetMode, setAppUpdateTargetMode] = useState<'All' | 'Single' | 'Selected'>('All');
 const [appUpdateTargetStudentId, setAppUpdateTargetStudentId] = useState<string>('');
 const [betaTestedStudentIds, setBetaTestedStudentIds] = useState<string[]>(() => {
  try {
   const saved = localStorage.getItem('eap_beta_tested_students');
   if (saved) return JSON.parse(saved);
  } catch (_e) {}
  return [];
 });

 // Custom Glassmorphic Confirmation Modal Dialog State
 const [confirmDialog, setConfirmDialog] = useState<{
  isOpen: boolean;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  isDanger?: boolean;
  onConfirm: () => void;
 } | null>(null);

 const showCustomConfirm = (
  title: string,
  message: string,
  onConfirm: () => void,
  options?: { confirmText?: string; cancelText?: string; isDanger?: boolean }
 ) => {
  sound.playTick();
  setConfirmDialog({
   isOpen: true,
   title,
   message,
   confirmText: options?.confirmText || 'Confirm',
   cancelText: options?.cancelText || 'Cancel',
   isDanger: options?.isDanger !== false,
   onConfirm: () => {
    setConfirmDialog(null);
    onConfirm();
   }
  });
 };

 // Notifications Hub State
 const [automatedAlerts, setAutomatedAlerts] = useState({
  morningRoutine: { enabled: true, time: '08:00', title: "Today's Schedule & Routine Alert" },
  eveningProgress: { enabled: true, time: '21:30', title: "Daily Progress Wrap-up & Streak" },
  countdown: { enabled: true, time: '10:00', title: "Admission Exam Milestone Countdown" },
  quotes: { enabled: true, intervalHours: 4, title: "Admission Motivation Booster" }
 });
 const [broadcastTargetAudience, setBroadcastTargetAudience] = useState<'All' | 'HSC 25' | 'HSC 26' | 'Individual'>('All');
 const [broadcastTargetStudentKey, setBroadcastTargetStudentKey] = useState<string>('');
 const [broadcastCategory, setBroadcastCategory] = useState<'Academic' | 'Routine' | 'Exam' | 'Personal' | 'Emergency'>('Academic');
 const [broadcastTitle, setBroadcastTitle] = useState<string>('Important Admission Update');
 const [broadcastMessage, setBroadcastMessage] = useState<string>('');
 const [broadcastActionUrl, setBroadcastActionUrl] = useState<string>('');
 const [broadcastActionButtonText, setBroadcastActionButtonText] = useState<string>('Open Routine');
 const [sentNotificationsHistory, setSentNotificationsHistory] = useState<any[]>([]);
 const [selectedStudentForInboxAudit, setSelectedStudentForInboxAudit] = useState<string>('');
 const [studentAuditedInbox, setStudentAuditedInbox] = useState<any[]>([]);
 const [isLoadingInbox, setIsLoadingInbox] = useState<boolean>(false);

 // Uploaded Routine Preview State
 const [uploadedRoutinePreview, setUploadedRoutinePreview] = useState<RoutineItem[] | null>(null);
 const [uploadedRoutineFileName, setUploadedRoutineFileName] = useState<string>('');
 const [uploadedRoutineTargetMode, setUploadedRoutineTargetMode] = useState<'Offline' | 'Online' | 'Individual'>('Offline');
 const [uploadedRoutineTargetStudentId, setUploadedRoutineTargetStudentId] = useState<string>('');
 const [isUploadingRoutineModalOpen, setIsUploadingRoutineModalOpen] = useState<boolean>(false);

 // Personal Student Notice Modal State
 const [isPersonalNoticeModalOpen, setIsPersonalNoticeModalOpen] = useState<boolean>(false);
 const [personalNoticeStudentId, setPersonalNoticeStudentId] = useState<string>('');
 const [personalNoticeStudentName, setPersonalNoticeStudentName] = useState<string>('');
 const [personalNoticeTitle, setPersonalNoticeTitle] = useState<string>('Personal Mentor Update');
 const [personalNoticeMessage, setPersonalNoticeMessage] = useState<string>('');
 const [personalNoticeUrl, setPersonalNoticeUrl] = useState<string>('');
 const [personalNoticeButtonText, setPersonalNoticeButtonText] = useState<string>('View Resource');

 // Quotes Tab State
 const [quotesList, setQuotesList] = useState<MotivationQuoteItem[]>(() => {
  const saved = localStorage.getItem('eap_admin_quotes');
  return saved ? JSON.parse(saved) : adminQuotesLibrary;
 });
 const [selectedQuoteCategory, setSelectedQuoteCategory] = useState<string>('all');
 const [customQuoteText, setCustomQuoteText] = useState<string>('');
 const [customQuoteAuthor, setCustomQuoteAuthor] = useState<string>('Admission Mentor');

 // Standalone Direct Push Notification State
 const [directNotifTitle, setDirectNotifTitle] = useState<string>('Notice for All Students');
 const [directNotifBody, setDirectNotifBody] = useState<string>('');
 const [directNotifCategory, setDirectNotifCategory] = useState<'Notice' | 'Alert' | 'Motivation' | 'Exam'>('Notice');
 const [directNotifUrl, setDirectNotifUrl] = useState<string>('');
 const [directNotifButtonText, setDirectNotifButtonText] = useState<string>('');
 const [directNotifRoute, setDirectNotifRoute] = useState<string>('');

 // Custom Glassmorphic Prompt / Text Input Modal Dialog State (replacing raw window.prompt)
 const [inputModalDialog, setInputModalDialog] = useState<{
  isOpen: boolean;
  title: string;
  subtitle?: string;
  initialValue?: string;
  placeholder?: string;
  confirmText?: string;
  onConfirm: (val: string) => void;
 } | null>(null);
 const [inputModalValue, setInputModalValue] = useState<string>('');

 const showCustomPrompt = (
  title: string,
  onConfirm: (val: string) => void,
  options?: { subtitle?: string; initialValue?: string; placeholder?: string; confirmText?: string }
 ) => {
  sound.playTick();
  setInputModalValue(options?.initialValue || '');
  setInputModalDialog({
   isOpen: true,
   title,
   subtitle: options?.subtitle,
   initialValue: options?.initialValue || '',
   placeholder: options?.placeholder || 'Enter name here...',
   confirmText: options?.confirmText || 'Save',
   onConfirm
  });
 };

 // Target Exam Countdown State with Live Date Picker
 const calculateDefaultExamDate = (days: number) => {
  return new Date(Date.now() + days * 86400000).toISOString().split('T')[0];
 };

 const [targetExamPreset, setTargetExamPreset] = useState<string>('BUET Preliminary Admission Test');
 const [targetExamCustomName, setTargetExamCustomName] = useState<string>('BUET Admission Test 2026');
 const [targetExamDays, setTargetExamDays] = useState<number>(120);
 const [targetExamDateInput, setTargetExamDateInput] = useState<string>(() => calculateDefaultExamDate(120));

 // Routines Data State (from localStorage or master dataset with full 60-day curriculum)
 const [offlineRoutines, setOfflineRoutines] = useState<RoutineItem[]>(() => {
  const saved = localStorage.getItem('eap_custom_offline_routines');
  if (saved) {
   const parsed = JSON.parse(saved);
   if (Array.isArray(parsed) && parsed.length >= 30) return parsed;
  }
  return (masterEapData.offlineRoutine as any) || initialRoutines;
 });
 const [onlineRoutines, setOnlineRoutines] = useState<RoutineItem[]>(() => {
  const saved = localStorage.getItem('eap_custom_online_routines');
  if (saved) {
   const parsed = JSON.parse(saved);
   if (Array.isArray(parsed) && parsed.length >= 30) return parsed;
  }
  return (masterEapData.onlineRoutine as any) || initialRoutines;
 });

 // Exams Data State
 const [offlineExams, setOfflineExams] = useState<any[]>(() => {
  const saved = localStorage.getItem('eap_custom_offline_exams');
  if (saved) {
   const parsed = JSON.parse(saved);
   if (Array.isArray(parsed) && parsed.length >= 20) return parsed;
  }
  return masterEapData.offlineExams || [];
 });
 const [onlineExams, setOnlineExams] = useState<any[]>(() => {
  const saved = localStorage.getItem('eap_custom_online_exams');
  if (saved) {
   const parsed = JSON.parse(saved);
   if (Array.isArray(parsed) && parsed.length >= 20) return parsed;
  }
  return masterEapData.onlineExams || [];
 });

 // Syllabus Data State
 const [subjects, setSubjects] = useState<Subject[]>(() => {
  const saved = localStorage.getItem('eap_custom_subjects');
  return saved ? JSON.parse(saved) : initialSubjects;
 });

 // REST API & Push Notification Config State
 const [apiEndpoint, setApiEndpoint] = useState<string>(() => {
  return localStorage.getItem('eap_rest_endpoint') || 'https://eap-tracker-default-rtdb.firebaseio.com/eap_data.json';
 });
 const [apiKey, setApiKey] = useState<string>(() => {
  return localStorage.getItem('eap_rest_api_key') || '';
 });
 const [workerUrl, setWorkerUrl] = useState<string>(() => {
  return localStorage.getItem('eap_cf_worker_url') || '';
 });
 const [workerSecret, setWorkerSecret] = useState<string>(() => {
  return localStorage.getItem('eap_cf_worker_secret') || '';
 });
 const [updateMessage, setUpdateMessage] = useState<string>(() => {
  return localStorage.getItem('eap_update_msg') || (masterEapData.updateMessage || 'New Class Routine & Exam Schedule Published! Tap to view updated dates & topics.');
 });
 const [lastSyncTime, setLastSyncTime] = useState<string>(() => {
  return localStorage.getItem('eap_last_sync_time') || 'Never';
 });
 const [isSyncing, setIsSyncing] = useState<boolean>(false);
 const [toastMessage, setToastMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

 /**
  * Dispatches instant FCM push notification to topic 'eap_updates' or 'eap_students'
  * This guarantees delivery to all students even when the app is completely closed or phone restarted.
  */
 const dispatchFCMTopicPush = async (
  title: string,
  body: string,
  topic: string = 'eap_updates',
  data: Record<string, string> = {}
 ): Promise<boolean> => {
  const url = (workerUrl || localStorage.getItem('eap_cf_worker_url') || '').trim();
  const secret = (workerSecret || localStorage.getItem('eap_cf_worker_secret') || '').trim();
  
  if (!url || !secret) {
   console.warn('FCM Push aborted: Cloudflare Worker URL or Secret not configured.');
   return false; // Return failure so the UI doesn't claim notification was sent
  }

  try {
   const payload = {
    topic: topic,
    title: title,
    message: body,
    data: {
     title: title,
     message: body,
     body: body,
     ...data,
     timestamp: String(Date.now())
    }
   };

   const response = await fetch(url, {
    method: 'POST',
    headers: {
     'Content-Type': 'application/json',
     'Authorization': `Bearer ${secret}`,
     'X-Admin-Key': `Bearer ${secret}`
    },
    body: JSON.stringify(payload)
   });

   if (!response.ok) {
    const errorText = await response.text();
    console.error('FCM Push Dispatch failed:', response.status, errorText);
    return false;
   }

   return true;
  } catch (e) {
   console.error('FCM Push Dispatch exception:', e);
   return false;
  }
 };

 // Modals
 const [editingRoutineItem, setEditingRoutineItem] = useState<{ index: number; item: RoutineItem } | null>(null);
 const [isAddingRoutine, setIsAddingRoutine] = useState<boolean>(false);

 const [editingExamItem, setEditingExamItem] = useState<{ index: number; item: any } | null>(null);
 const [isAddingExam, setIsAddingExam] = useState<boolean>(false);

 const [isAddingSubject, setIsAddingSubject] = useState<boolean>(false);
 const [newSubName, setNewSubName] = useState<string>('');

 const [chapterTarget, setChapterTarget] = useState<{ subId: string; paperId: string } | null>(null);
 const [newChapterName, setNewChapterName] = useState<string>('');

 // Persist State Changes
 useEffect(() => {
  localStorage.setItem('eap_admin_quotes', JSON.stringify(quotesList));
 }, [quotesList]);

 // Persist State Changes
 useEffect(() => {
  localStorage.setItem('eap_custom_offline_routines', JSON.stringify(offlineRoutines));
 }, [offlineRoutines]);

 useEffect(() => {
  localStorage.setItem('eap_custom_online_routines', JSON.stringify(onlineRoutines));
 }, [onlineRoutines]);

 useEffect(() => {
  localStorage.setItem('eap_custom_offline_exams', JSON.stringify(offlineExams));
 }, [offlineExams]);

 useEffect(() => {
  localStorage.setItem('eap_custom_online_exams', JSON.stringify(onlineExams));
 }, [onlineExams]);

 useEffect(() => {
  localStorage.setItem('eap_custom_subjects', JSON.stringify(subjects));
 }, [subjects]);

 const showNotification = (text: string, type: 'success' | 'error' = 'success') => {
  setToastMessage({ text, type });
  setTimeout(() => setToastMessage(null), 4000);
 };

 const handleLogin = async (e: React.FormEvent) => {
  e.preventDefault();
  setAuthLoading(true);
  const res = await adminAuth.verifyPassword(passwordInput);
  setAuthLoading(false);

  if (res.success) {
   sound.playSuccess();
   setIsAuthenticated(true);
   setAuthError('');
   setPasswordInput('');
   setLockoutSec(0);
   showNotification('Admin Authenticated Successfully');
  } else {
   sound.playNegative();
   setAuthError(res.error || 'Incorrect Admin Password');
   if (res.lockoutRemainingSeconds) {
    setLockoutSec(res.lockoutRemainingSeconds);
   }
  }
 };

  const handleGoogleSignIn = async () => {
  setAuthLoading(true);
  setAuthError('');
  const res = await googleDriveService.signInWithGoogle();
  setAuthLoading(false);

  if (res.success && res.user) {
   sound.playSuccess();
   setGoogleAdminUser(res.user);
   setIsAuthenticated(true);
   showNotification(`Welcome, ${res.user.displayName || res.user.email || 'Admin'}!`);
  } else {
   sound.playNegative();
   setAuthError(res.error || 'Google Sign-In failed');
  }
 };

 const handlePerformGoogleDriveBackup = async () => {
  setIsGdriveBackingUp(true);
  const payload = generatePayload();
  const res = await googleDriveService.uploadBackupToGoogleDrive(payload);
  setIsGdriveBackingUp(false);
  if (res.success) {
   sound.playSuccess();
   setGdriveLastTime(googleDriveService.getLastBackupTime());
   setGdriveLastSize(googleDriveService.getLastBackupSize());
   showNotification('Master backup uploaded successfully to Google Drive!');
   loadGdriveBackups();
  } else {
   sound.playNegative();
   showNotification(res.error || 'Failed to upload to Google Drive', 'error');
  }
 };

 const loadGdriveBackups = async () => {
  setIsGdriveLoading(true);
  const res = await googleDriveService.listGoogleDriveBackups();
  setIsGdriveLoading(false);
  if (res.success) {
   setGdriveBackupsList(res.backups);
  }
 };

 const handleRestoreFromGoogleDrive = async (item: GoogleDriveBackupItem) => {
  setIsGdriveLoading(true);
  const res = await googleDriveService.downloadBackupFromGoogleDrive(item.id);
  setIsGdriveLoading(false);
  if (res.success && res.data) {
   sound.playSuccess();
   if (res.data.routines) {
    if (res.data.routines.offline) setOfflineRoutines(res.data.routines.offline);
    if (res.data.routines.online) setOnlineRoutines(res.data.routines.online);
   }
   if (res.data.exams) {
    if (res.data.exams.offline) setOfflineExams(res.data.exams.offline);
    if (res.data.exams.online) setOnlineExams(res.data.exams.online);
   }
   if (res.data.subjects) {
    setSubjects(res.data.subjects);
   }
   setIsRestoreModalOpen(false);
   showNotification(`Restored backup "${item.name}" from Google Drive!`);
  } else {
   sound.playNegative();
   showNotification(res.error || 'Failed to download backup', 'error');
  }
 };

 const handleLogout = async () => {
  await firebaseAuthService.signOutAdmin();
  googleDriveService.clearAccessToken();
  adminAuth.logout();
  setGoogleAdminUser(null);
  setIsAuthenticated(false);
  showNotification('Admin Portal Locked');
 };

 const handleChangePassword = async (e: React.FormEvent) => {
  e.preventDefault();
  if (newPasswordInput !== confirmPasswordInput) {
   setChangePasswordError('New passwords do not match.');
   return;
  }
  const res = await adminAuth.changePassword(currentPasswordInput, newPasswordInput);
  if (res.success) {
   sound.playSuccess();
   showNotification('Master Admin Password Updated');
   setIsChangePasswordModalOpen(false);
   setCurrentPasswordInput('');
   setNewPasswordInput('');
   setConfirmPasswordInput('');
   setChangePasswordError('');
  } else {
   sound.playNegative();
   setChangePasswordError(res.error || 'Failed to update password.');
  }
 };

 // Fetch Student Telemetry with Deduplication
 const fetchStudents = async () => {
  setIsLoadingStudents(true);
  try {
   let baseUrl = apiEndpoint.trim().replace(/\/eap_data\.json.*$/, '').replace(/\/+$/, '');
   if (!baseUrl.includes('firebaseio.com')) {
    baseUrl = 'https://eap-tracker-default-rtdb.firebaseio.com';
   }
   const targetUrl = `${baseUrl}/students.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`;
   const res = await fetch(targetUrl);
   if (res.ok) {
    const data = await res.json();
    if (data) {
     const rawEntries: { key: string; summary: StudentTelemetry }[] = [];
     let ghostFound = 0;

     Object.entries(data).forEach(([key, val]: [string, any]) => {
      const summary = val?.profile_summary || (val?.studentId ? val : null);
      if (summary) {
       rawEntries.push({ key, summary });
      }
      if (key.startsWith('student_') || key.includes('_at_')) {
       ghostFound++;
      }
     });

     // Smart deduplication: Merge duplicate records by email or Candidate ID
     const mergedMap = new Map<string, StudentTelemetry>();

     rawEntries.forEach(({ key, summary }) => {
      const emailKey = summary.email?.trim().toLowerCase();
      const dedupKey = emailKey || summary.studentId || key;

      if (mergedMap.has(dedupKey)) {
       const existing = mergedMap.get(dedupKey)!;
       const isCurrentOfficialCandidate = summary.studentId?.startsWith('EAP-');
       const isExistingOfficialCandidate = existing.studentId?.startsWith('EAP-');

       if (!isExistingOfficialCandidate && isCurrentOfficialCandidate) {
        mergedMap.set(dedupKey, summary);
       } else if (isCurrentOfficialCandidate && isExistingOfficialCandidate) {
        // Keep the one with latest activity or more completed chapters
        if ((summary.lastActiveEpoch || 0) >= (existing.lastActiveEpoch || 0)) {
         mergedMap.set(dedupKey, summary);
        }
       }
      } else {
       mergedMap.set(dedupKey, summary);
      }
     });

     setStudentsList(Array.from(mergedMap.values()));
     setDetectedGhostCount(ghostFound);
    } else {
     setStudentsList([]);
     setDetectedGhostCount(0);
    }
   }
  } catch (e: any) {
   console.error('Error fetching students:', e);
  } finally {
   setIsLoadingStudents(false);
  }
 };

 // 1-Click Purge Duplicate & Ghost Student Entries from Firebase RTDB
 const handlePurgeGhostStudents = () => {
  showCustomConfirm(
   'Purge Ghost Duplicates?',
   'Purge all duplicate and temporary pre-setup ghost records (e.g. student_MODEL, email keys) from Firebase RTDB?',
   async () => {
    setIsSyncing(true);
    try {
     const res = await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`);
     if (res.ok) {
      const data = await res.json();
      if (data) {
       let purgedCount = 0;
       for (const [key, val] of Object.entries<any>(data)) {
        const studentId = val?.profile_summary?.studentId || val?.studentId || '';
        const isTempKey = key.startsWith('student_') || key.includes('_at_');
        const isInvalidId = !studentId.startsWith('EAP-');

        if (isTempKey || isInvalidId) {
         await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${key}.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
          method: 'DELETE'
         });
         purgedCount++;
        }
       }
       sound.playSuccess();
       showNotification(`Cleaned ${purgedCount} ghost/duplicate student records!`);
       fetchStudents();
      }
     }
    } catch (_e) {
     sound.playNegative();
     showNotification('Failed to purge ghost records', 'error');
    } finally {
     setIsSyncing(false);
    }
   },
   { confirmText: 'Purge Records', cancelText: 'Cancel', isDanger: true }
  );
 };

 // Delete Individual Student Record
 const handleDeleteStudent = (studentId: string) => {
  showCustomConfirm(
   `Remove Student ${studentId}?`,
   `Are you sure you want to completely remove student "${studentId}" and all associated cloud telemetry from Firebase RTDB?`,
   async () => {
    setIsSyncing(true);
    try {
     const studentKey = studentId.replace(/[^a-zA-Z0-9]/g, '_');
     await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
      method: 'DELETE'
     });
     sound.playSuccess();
     showNotification(`Student ${studentId} removed successfully.`);
     fetchStudents();
     if (selectedStudentForDetail?.studentId === studentId) {
      setSelectedStudentForDetail(null);
     }
    } catch (_e) {
     sound.playNegative();
     showNotification('Failed to delete student record', 'error');
    } finally {
     setIsSyncing(false);
    }
   },
   { confirmText: 'Remove Student', cancelText: 'Cancel', isDanger: true }
  );
 };

 // Fetch Sent Notifications History & Global Latest
 const fetchSentNotifications = async () => {
  try {
   const res = await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/sent_admin_notices.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`);
   if (res.ok) {
    const data = await res.json();
    if (data) {
     const list = Object.entries(data).map(([id, val]: [string, any]) => ({
      id,
      ...val
     }));
     list.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
     setSentNotificationsHistory(list);
    } else {
     setSentNotificationsHistory([]);
    }
   }
  } catch (_e) {}
 };

 // Dispatch Broadcast or Targeted Push Notification
 const handleDispatchBroadcastNotification = async () => {
  if (!broadcastMessage.trim()) {
   showNotification('Please enter notification body message', 'error');
   return;
  }
  setIsSyncing(true);
  try {
   const notifPayload = {
    title: broadcastTitle.trim() || 'Important Admission Update',
    message: broadcastMessage.trim(),
    category: broadcastCategory,
    targetAudience: broadcastTargetAudience,
    targetStudentId: broadcastTargetStudentKey,
    actionUrl: broadcastActionUrl.trim() || '',
    actionButtonText: broadcastActionButtonText.trim() || 'Open Routine',
    timestamp: Date.now()
   };

   if (broadcastTargetAudience === 'Individual' && broadcastTargetStudentKey) {
    // Send to specific student's personal inbox
    const studentKey = broadcastTargetStudentKey.replace(/[^a-zA-Z0-9]/g, '_');
    await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/personal_notice.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
     method: 'PUT',
     headers: { 'Content-Type': 'application/json' },
     body: JSON.stringify(notifPayload)
    });
   } else {
    // Broadcast to Global Latest
    await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/global_notifications/latest.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
     method: 'PUT',
     headers: { 'Content-Type': 'application/json' },
     body: JSON.stringify(notifPayload)
    });
   }

   // Record in Sent Notifications Log
   await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/sent_admin_notices.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(notifPayload)
   });

   // Dispatch FCM Push so closed / killed Android apps get instant notification
   let pushSuccess = true;
   if (broadcastTargetAudience !== 'Individual') {
    pushSuccess = await dispatchFCMTopicPush(
     notifPayload.title,
     notifPayload.message,
     'eap_updates',
     {
      category: notifPayload.category,
      actionUrl: notifPayload.actionUrl,
      actionButtonText: notifPayload.actionButtonText,
      targetAudience: notifPayload.targetAudience
     }
    );
   }

   if (pushSuccess) {
    sound.playSuccess();
    showNotification(`Push notification dispatched to ${broadcastTargetAudience === 'Individual' ? broadcastTargetStudentKey : broadcastTargetAudience}!`);
   } else {
    sound.playNegative();
    showNotification(`Data published successfully, but notification sending failed.`, 'error');
   }
   setBroadcastMessage('');
   fetchSentNotifications();
  } catch (_e) {
   sound.playNegative();
   showNotification('Failed to dispatch notification', 'error');
  } finally {
   setIsSyncing(false);
  }
 };

 // Dispatch App Update Broadcast to All Devices
 const handleBroadcastAppUpdate = async () => {
  if (!appUpdateApkUrl.trim()) {
   showNotification('Please enter direct APK download link', 'error');
   return;
  }
  showCustomConfirm(
   'Broadcast App Update?',
   `Broadcast EAP Tracker ${appUpdateVersionName} (Build ${appUpdateVersionCode}) to all student devices? Students will be prompted with in-app auto-download and 1-tap install.`,
   async () => {
    setIsSyncing(true);
    try {
     const updatePayload = {
      versionName: appUpdateVersionName.trim(),
      versionCode: Number(appUpdateVersionCode) || 2,
      apkUrl: appUpdateApkUrl.trim(),
      title: appUpdateTitle.trim() || ' New Update Available',
      changelog: appUpdateChangelog.trim(),
      isForceUpdate: Boolean(appUpdateIsForce),
      publishedAt: Date.now()
     };

     // 1. Write to RTDB /app_config/latest_update.json
     await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/app_config/latest_update.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(updatePayload)
     });

     // 2. Log in sent admin notices
     await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/sent_admin_notices.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
       title: updatePayload.title,
       message: updatePayload.changelog,
       category: 'Update',
       targetAudience: 'All Students',
       actionUrl: updatePayload.apkUrl,
       actionButtonText: 'Update Now',
       timestamp: Date.now()
      })
     });

     // 3. Dispatch High-Priority FCM Push with app_update type
     await dispatchFCMTopicPush(
      updatePayload.title,
      updatePayload.changelog.replace(/\n/g, ' '),
      'eap_updates',
      {
       category: 'Update',
       type: 'app_update',
       apkUrl: updatePayload.apkUrl,
       versionName: updatePayload.versionName,
       versionCode: String(updatePayload.versionCode),
       isForce: String(updatePayload.isForceUpdate),
       actionUrl: updatePayload.apkUrl,
       actionButtonText: 'Update Now'
      }
     );

     sound.playSuccess();
     showNotification(` Update ${appUpdateVersionName} broadcasted to all students!`);
     fetchSentNotifications();
    } catch (_e) {
     sound.playNegative();
     showNotification('Failed to broadcast app update', 'error');
    } finally {
     setIsSyncing(false);
    }
   },
   { confirmText: 'Broadcast Update', cancelText: 'Cancel', isDanger: false }
  );
 };

 // Delete / Recall Notification from Firebase RTDB
 const handleDeleteSentNotification = (notifId: string) => {
  showCustomConfirm(
   'Recall Notification?',
   'Recall and delete this notification from cloud broadcast history?',
   async () => {
    setIsSyncing(true);
    try {
     await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/sent_admin_notices/${notifId}.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
      method: 'DELETE'
     });
     sound.playSuccess();
     showNotification('Notification deleted successfully.');
     fetchSentNotifications();
    } catch (_e) {
     sound.playNegative();
     showNotification('Failed to delete notification', 'error');
    } finally {
     setIsSyncing(false);
    }
   },
   { confirmText: 'Recall Notification', cancelText: 'Cancel', isDanger: true }
  );
 };

 // Dispatch Targeted Beta / Test App Update to Specific Student(s)
 const handleSendTargetedAppUpdate = async (targetStudentIds: string[]) => {
  if (!appUpdateApkUrl.trim()) {
   showNotification('Please enter direct APK download link', 'error');
   return;
  }
  if (targetStudentIds.length === 0) {
   showNotification('Please select at least one test student', 'error');
   return;
  }

  showCustomConfirm(
   'Deploy Test App Update?',
   `Deploy test update EAP Tracker ${appUpdateVersionName} (Build ${appUpdateVersionCode}) to ${targetStudentIds.length} candidate(s)? They will receive an in-app test update prompt.`,
   async () => {
    setIsSyncing(true);
    try {
     const updatePayload = {
      versionName: appUpdateVersionName.trim(),
      versionCode: Number(appUpdateVersionCode) || 2,
      apkUrl: appUpdateApkUrl.trim(),
      title: appUpdateTitle.trim() || ` Test Update Available (${appUpdateVersionName})`,
      changelog: appUpdateChangelog.trim(),
      isForceUpdate: Boolean(appUpdateIsForce),
      publishedAt: Date.now()
     };

     for (const sid of targetStudentIds) {
      const studentKey = cleanStudentKey(sid);
      await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/app_update.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
       method: 'PUT',
       headers: { 'Content-Type': 'application/json' },
       body: JSON.stringify(updatePayload)
      });

      // Dispatch Targeted FCM Push for this student
      await dispatchFCMTopicPush(
       updatePayload.title,
       updatePayload.changelog.replace(/\n/g, ' '),
       'eap_updates',
       {
        category: 'Update',
        type: 'app_update',
        targetStudentId: sid,
        apkUrl: updatePayload.apkUrl,
        versionName: updatePayload.versionName,
        versionCode: String(updatePayload.versionCode),
        isForce: String(updatePayload.isForceUpdate),
        actionUrl: updatePayload.apkUrl,
        actionButtonText: 'Update Now'
       }
      );
     }

     // Mark students in betaTestedStudentIds
     setBetaTestedStudentIds((prev) => {
      const next = Array.from(new Set([...prev, ...targetStudentIds]));
      localStorage.setItem('eap_beta_tested_students', JSON.stringify(next));
      return next;
     });

     sound.playSuccess();
     showNotification(` Test release ${appUpdateVersionName} deployed to ${targetStudentIds.length} candidate(s)!`);
    } catch (_e) {
     sound.playNegative();
     showNotification('Failed to deploy test release', 'error');
    } finally {
     setIsSyncing(false);
    }
   },
   { confirmText: 'Deploy Test Update', cancelText: 'Cancel', isDanger: false }
  );
 };

 // Mass Apply Preset to Multiple Students
 const handleMassApplyPreset = async (preset: PresetTemplate, targetStudentIds: string[]) => {
  if (targetStudentIds.length === 0) {
   showNotification('Please select at least one student', 'error');
   return;
  }

  showCustomConfirm(
   `Assign "${preset.name}" to ${targetStudentIds.length} Students?`,
   `This will deploy the preset (${preset.category}) to all ${targetStudentIds.length} selected candidates in Firebase RTDB and trigger instant FCM notifications on their phones.`,
   async () => {
    setIsSyncing(true);
    try {
     for (const sid of targetStudentIds) {
      const studentKey = cleanStudentKey(sid);

      // Deploy Syllabus if present in preset
      if (preset.data.subjects && preset.data.subjects.length > 0) {
       await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/custom_syllabus.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
         subjects: preset.data.subjects,
         updatedEpoch: Date.now()
        })
       });
      }

      // Deploy Routine if present in preset
      if (preset.data.routineDays && preset.data.routineDays.length > 0) {
       const path = preset.data.routineMode === 'Online' ? 'custom_online_routine' : 'custom_routine';
       await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/${path}.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(preset.data.routineDays)
       });
      }

      // Deploy Exams if present in preset
      if (preset.data.exams && preset.data.exams.length > 0) {
       await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/custom_exams.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(preset.data.exams)
       });
      }

      // Send High-Priority FCM Push to student
      await dispatchFCMTopicPush(
       ` New Academic Track: ${preset.name}`,
       `Your mentor has assigned the "${preset.name}" curriculum to your account. Open EAP Tracker to sync!`,
       'eap_general',
       {
        category: 'Notice',
        type: 'preset_assignment',
        targetStudentId: sid,
        presetId: preset.id,
        presetName: preset.name
       }
      );
     }

     sound.playSuccess();
     showNotification(` Assigned "${preset.name}" to ${targetStudentIds.length} students!`);
     setIsMassAssignModalOpen(false);
     setSelectedStudentRosterIds([]);
    } catch (_e) {
     sound.playNegative();
     showNotification('Failed to assign preset to students', 'error');
    } finally {
     setIsSyncing(false);
    }
   },
   { confirmText: 'Deploy to All', cancelText: 'Cancel', isDanger: false }
  );
 };

 // Mass Reset Selected Students back to Global Master
 const handleMassResetStudentsToGlobal = async (targetStudentIds: string[]) => {
  if (targetStudentIds.length === 0) return;
  showCustomConfirm(
   `Reset ${targetStudentIds.length} Students to Global Master?`,
   `This will delete custom syllabus, custom routines, and custom exams for all ${targetStudentIds.length} selected students, restoring them to the default global schedule.`,
   async () => {
    setIsSyncing(true);
    try {
     for (const sid of targetStudentIds) {
      const studentKey = cleanStudentKey(sid);
      await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/custom_syllabus.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, { method: 'DELETE' });
      await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/custom_routine.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, { method: 'DELETE' });
      await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/custom_online_routine.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, { method: 'DELETE' });
      await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/custom_exams.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, { method: 'DELETE' });
     }
     sound.playSuccess();
     showNotification(`Reset ${targetStudentIds.length} students to Global Master!`);
     setSelectedStudentRosterIds([]);
    } catch (_e) {
     sound.playNegative();
     showNotification('Failed to reset students', 'error');
    } finally {
     setIsSyncing(false);
    }
   },
   { confirmText: 'Reset to Global', cancelText: 'Cancel', isDanger: true }
  );
 };

 const handleSaveCurrentAsPreset = (name: string, category: 'Syllabus' | 'Routine' | 'Exams' | 'FullTrack', description?: string) => {
  if (!name.trim()) return;
  const newPreset: PresetTemplate = {
   id: `preset_${Date.now()}`,
   name: name.trim(),
   category,
   description: description || `Custom ${category} preset created on ${new Date().toLocaleDateString()}`,
   createdAt: Date.now(),
   data: {
    subjects: category === 'Syllabus' || category === 'FullTrack' ? [...subjects] : undefined,
    routineDays: category === 'Routine' || category === 'FullTrack' ? (routineMode === 'Offline' ? [...offlineRoutines] : [...onlineRoutines]) : undefined,
    routineMode,
    exams: category === 'Exams' || category === 'FullTrack' ? (examMode === 'Offline' ? [...offlineExams] : [...onlineExams]) : undefined
   }
  };
  setPresetsList((prev) => {
   const next = [newPreset, ...prev];
   localStorage.setItem('eap_admin_presets', JSON.stringify(next));
   return next;
  });
  sound.playSuccess();
  showNotification(`Saved preset: "${name}"`);
 };

 const handleDeletePreset = (presetId: string) => {
  showCustomConfirm(
   'Delete Preset Template?',
   'Are you sure you want to delete this preset template from your library?',
   () => {
    setPresetsList((prev) => {
     const next = prev.filter(p => p.id !== presetId);
     localStorage.setItem('eap_admin_presets', JSON.stringify(next));
     return next;
    });
    sound.playSuccess();
    showNotification('Preset deleted');
   },
   { confirmText: 'Delete Preset', cancelText: 'Cancel', isDanger: true }
  );
 };

 // Audit Student Live Notification Inbox
 const handleAuditStudentInbox = async (studentId: string) => {
  if (!studentId) return;
  setSelectedStudentForInboxAudit(studentId);
  setIsLoadingInbox(true);
  try {
   const studentKey = studentId.replace(/[^a-zA-Z0-9]/g, '_');
   const res = await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/personal_notice.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`);
   const list: any[] = [];
   if (res.ok) {
    const personal = await res.json();
    if (personal && personal.message) {
     list.push({ ...personal, type: 'Personal Mentor Notice' });
    }
   }
   // Also get global notification
   const globalRes = await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/global_notifications/latest.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`);
   if (globalRes.ok) {
    const globalData = await globalRes.json();
    if (globalData && globalData.message) {
     list.push({ ...globalData, type: 'Global Academic Announcement' });
    }
   }
   setStudentAuditedInbox(list);
  } catch (_e) {
   setStudentAuditedInbox([]);
  } finally {
   setIsLoadingInbox(false);
  }
 };

 // Clear or Delete Student Personal Notices from Firebase RTDB
 const handleClearStudentInbox = async (studentId: string) => {
  if (!studentId) return;
  showCustomConfirm(
   'Clear Personal Notices?',
   `Are you sure you want to delete all personal mentor notices for candidate "${studentId}" from the live database?`,
   async () => {
    try {
     const studentKey = cleanStudentKey(studentId);
     await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/personal_notice.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
      method: 'DELETE'
     });
     setStudentAuditedInbox((prev) => prev.filter((item) => item.type !== 'Personal Mentor Notice'));
     sound.playSuccess();
     showNotification(`Personal notices cleared for ${studentId}`);
    } catch (_e) {
     sound.playNegative();
     showNotification('Failed to clear notices', 'error');
    }
   },
   { confirmText: 'Clear Notices', cancelText: 'Cancel', isDanger: true }
  );
 };

 // Save Automated Notification Schedules
 const handleSaveAutomatedRules = async () => {
  setIsSyncing(true);
  try {
   await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/automated_notification_rules.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(automatedAlerts)
   });
   sound.playSuccess();
   showNotification('Automated notification schedules saved to cloud!');
  } catch (_e) {
   sound.playNegative();
   showNotification('Failed to save automated rules', 'error');
  } finally {
   setIsSyncing(false);
  }
 };

 // Fetch Active Target Exam Countdown from Cloud
 const fetchActiveTargetExam = async () => {
  try {
   let baseUrl = apiEndpoint.trim().replace(/\/eap_data\.json.*$/, '').replace(/\/+$/, '');
   if (!baseUrl.includes('firebaseio.com')) {
    baseUrl = 'https://eap-tracker-default-rtdb.firebaseio.com';
   }
   const targetUrl = `${baseUrl}/target_exam_config.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`;
   const res = await fetch(targetUrl);
   if (res.ok) {
    const data = await res.json();
    if (data && data.examName && data.examEpoch) {
     const diffDays = Math.max(1, Math.ceil((data.examEpoch - Date.now()) / (1000 * 60 * 60 * 24)));
     setTargetExamPreset(data.examName);
     setTargetExamDays(diffDays);
    }
   }
  } catch (_e) {}
 };

 // Open Student Detail and fetch their custom routine & exams if assigned
 const handleOpenStudentDetail = async (student: StudentTelemetry) => {
  setSelectedStudentForDetail(student);
  setIsLoadingCustomRoutine(true);
  try {
   const studentKey = cleanStudentKey(student.studentId);
   
   // 1. Fetch custom routines (Offline & Online)
   const resRoutine = await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/custom_routine.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`);
   if (resRoutine.ok) {
    const data = await resRoutine.json();
    if (data) {
     if (data.offlineRoutine && Array.isArray(data.offlineRoutine)) {
      setStudentCustomRoutine(data.offlineRoutine);
     } else if (data.routineDays && Array.isArray(data.routineDays)) {
      setStudentCustomRoutine(data.routineDays);
     } else {
      setStudentCustomRoutine(null);
     }

     if (data.onlineRoutine && Array.isArray(data.onlineRoutine)) {
      setStudentCustomOnlineRoutine(data.onlineRoutine);
     } else {
      setStudentCustomOnlineRoutine(null);
     }
    } else {
     setStudentCustomRoutine(null);
     setStudentCustomOnlineRoutine(null);
    }
   } else {
    setStudentCustomRoutine(null);
    setStudentCustomOnlineRoutine(null);
   }

   // 2. Fetch custom exams (Offline & Online)
   const resExams = await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/custom_exams.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`);
   if (resExams.ok) {
    const data = await resExams.json();
    if (data) {
     if (data.offlineExams && Array.isArray(data.offlineExams)) {
      setStudentCustomOfflineExams(data.offlineExams);
     } else if (data.examList && Array.isArray(data.examList)) {
      setStudentCustomOfflineExams(data.examList);
     } else {
      setStudentCustomOfflineExams(null);
     }

     if (data.onlineExams && Array.isArray(data.onlineExams)) {
      setStudentCustomOnlineExams(data.onlineExams);
     } else {
      setStudentCustomOnlineExams(null);
     }
    } else {
     setStudentCustomOfflineExams(null);
     setStudentCustomOnlineExams(null);
    }
   } else {
    setStudentCustomOfflineExams(null);
    setStudentCustomOnlineExams(null);
   }

   // 3. Fetch custom syllabus (if student has custom subjects/toggles)
   setIsLoadingCustomSyllabus(true);
   const resSyllabus = await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/custom_syllabus.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`);
   if (resSyllabus.ok) {
    const data = await resSyllabus.json();
    if (data && data.subjects && Array.isArray(data.subjects)) {
     setStudentCustomSyllabus(data.subjects);
    } else {
     setStudentCustomSyllabus(null);
    }
   } else {
    setStudentCustomSyllabus(null);
   }
  } catch (_e) {
   setStudentCustomRoutine(null);
   setStudentCustomOnlineRoutine(null);
   setStudentCustomOfflineExams(null);
   setStudentCustomOnlineExams(null);
   setStudentCustomSyllabus(null);
  } finally {
   setIsLoadingCustomRoutine(false);
   setIsLoadingCustomSyllabus(false);
   setSelectedStudentRoutineIndices([]);
   setSelectedStudentExamIndices([]);
   setSelectedStudentSubjectIndices([]);
  }
 };

 // Reset Student Custom Routine to Master Global Routine
 const handleResetStudentCustomRoutine = (studentId: string) => {
  showCustomConfirm(
   'Reset Routine to Global?',
   `Are you sure you want to reset custom routines for student "${studentId}" back to Global Master?`,
   async () => {
    setIsSyncing(true);
    try {
     const studentKey = cleanStudentKey(studentId);
     await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/custom_routine.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
      method: 'DELETE'
     });
     setStudentCustomRoutine(null);
     setStudentCustomOnlineRoutine(null);
     sound.playSuccess();
     showNotification(`Reset ${studentId} back to Global Master Routine!`);
    } catch (_e) {
     sound.playNegative();
     showNotification('Failed to reset custom routine', 'error');
    } finally {
     setIsSyncing(false);
    }
   },
   { confirmText: 'Reset to Global', cancelText: 'Cancel', isDanger: true }
  );
 };

 // Reset Student Custom Exams to Master Global Exams
 const handleResetStudentCustomExams = (studentId: string) => {
  showCustomConfirm(
   'Reset Exams to Global?',
   `Are you sure you want to reset custom exams for student "${studentId}" back to Global Master?`,
   async () => {
    setIsSyncing(true);
    try {
     const studentKey = cleanStudentKey(studentId);
     await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/custom_exams.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
      method: 'DELETE'
     });
     setStudentCustomOfflineExams(null);
     setStudentCustomOnlineExams(null);
     sound.playSuccess();
     showNotification(`Reset ${studentId} back to Global Master Exams!`);
    } catch (_e) {
     sound.playNegative();
     showNotification('Failed to reset custom exams', 'error');
    } finally {
     setIsSyncing(false);
    }
   },
   { confirmText: 'Reset to Global', cancelText: 'Cancel', isDanger: true }
  );
 };

 // Save & Deploy Custom Syllabus to Student
 const handleSaveStudentCustomSyllabus = async (studentId: string, customSubjects: Subject[], noticeText?: string) => {
  setIsSyncing(true);
  try {
   const studentKey = cleanStudentKey(studentId);
   const payload = {
    subjects: customSubjects,
    updatedEpoch: Date.now()
   };

   await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/custom_syllabus.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
   });

   setStudentCustomSyllabus(customSubjects);

   // Dispatch High-Priority Push
   await dispatchFCMTopicPush(
    ' Custom Syllabus Assigned',
    noticeText || `Your mentor has customized your subject syllabus with ${customSubjects.length} subjects!`,
    'eap_students',
    { category: 'Syllabus', type: 'custom_syllabus', targetStudentId: studentId }
   );

   sound.playSuccess();
   showNotification(` Custom syllabus deployed live to ${selectedStudentForDetail?.name || studentId}!`);
  } catch (_e) {
   sound.playNegative();
   showNotification('Failed to deploy custom syllabus', 'error');
  } finally {
   setIsSyncing(false);
  }
 };

 // Reset Student Syllabus back to Global Master
 const handleResetStudentCustomSyllabus = (studentId: string) => {
  showCustomConfirm(
   'Reset Syllabus to Global Master?',
   `Are you sure you want to remove the custom syllabus and restore the Global Master syllabus for ${selectedStudentForDetail?.name || studentId}?`,
   async () => {
    setIsSyncing(true);
    try {
     const studentKey = cleanStudentKey(studentId);
     await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/custom_syllabus.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
      method: 'DELETE'
     });
     setStudentCustomSyllabus(null);

     // Dispatch High-Priority Push
     await dispatchFCMTopicPush(
      ' Syllabus Reverted to Global',
      'Your subject syllabus has been synchronized back with the Global Master Curriculum.',
      'eap_students',
      { category: 'Syllabus', type: 'custom_syllabus', targetStudentId: studentId }
     );

     sound.playSuccess();
     showNotification(`Restored Global Master Syllabus for ${studentId}.`);
    } catch (_e) {
     sound.playNegative();
     showNotification('Failed to reset custom syllabus', 'error');
    } finally {
     setIsSyncing(false);
    }
   },
   { confirmText: 'Reset to Global', cancelText: 'Cancel', isDanger: true }
  );
 };

 // Global & Student Subject Reordering Helpers
 const moveSubjectUp = (index: number, isStudentCustom: boolean = false) => {
  if (index <= 0) return;
  if (isStudentCustom) {
   setStudentCustomSyllabus((prev) => {
    const list = prev && prev.length > 0 ? [...prev] : [...subjects];
    const temp = list[index - 1];
    list[index - 1] = list[index];
    list[index] = temp;
    return list;
   });
  } else {
   setSubjects((prev) => {
    const next = [...prev];
    const temp = next[index - 1];
    next[index - 1] = next[index];
    next[index] = temp;
    return next;
   });
  }
 };

 const moveSubjectDown = (index: number, isStudentCustom: boolean = false) => {
  if (isStudentCustom) {
   setStudentCustomSyllabus((prev) => {
    const list = prev && prev.length > 0 ? [...prev] : [...subjects];
    if (index >= list.length - 1) return prev;
    const temp = list[index + 1];
    list[index + 1] = list[index];
    list[index] = temp;
    return list;
   });
  } else {
   setSubjects((prev) => {
    if (index >= prev.length - 1) return prev;
    const next = [...prev];
    const temp = next[index + 1];
    next[index + 1] = next[index];
    next[index] = temp;
    return next;
   });
  }
 };

 const moveChapterUp = (subIdx: number, papIdx: number, chapIdx: number, isStudentCustom: boolean = false) => {
  if (chapIdx <= 0) return;
  if (isStudentCustom) {
   setStudentCustomSyllabus((prev) => {
    const list = prev && prev.length > 0 ? [...prev] : [...subjects];
    const papers = [...list[subIdx].papers];
    const chapters = [...papers[papIdx].chapters];
    const temp = chapters[chapIdx - 1];
    chapters[chapIdx - 1] = chapters[chapIdx];
    chapters[chapIdx] = temp;
    papers[papIdx] = { ...papers[papIdx], chapters };
    list[subIdx] = { ...list[subIdx], papers };
    return list;
   });
  } else {
   setSubjects((prev) => {
    const next = [...prev];
    const papers = [...next[subIdx].papers];
    const chapters = [...papers[papIdx].chapters];
    const temp = chapters[chapIdx - 1];
    chapters[chapIdx - 1] = chapters[chapIdx];
    chapters[chapIdx] = temp;
    papers[papIdx] = { ...papers[papIdx], chapters };
    next[subIdx] = { ...next[subIdx], papers };
    return next;
   });
  }
 };

 const moveChapterDown = (subIdx: number, papIdx: number, chapIdx: number, isStudentCustom: boolean = false) => {
  if (isStudentCustom) {
   setStudentCustomSyllabus((prev) => {
    const list = prev && prev.length > 0 ? [...prev] : [...subjects];
    const papers = [...list[subIdx].papers];
    const chapters = [...papers[papIdx].chapters];
    if (chapIdx >= chapters.length - 1) return prev;
    const temp = chapters[chapIdx + 1];
    chapters[chapIdx + 1] = chapters[chapIdx];
    chapters[chapIdx] = temp;
    papers[papIdx] = { ...papers[papIdx], chapters };
    list[subIdx] = { ...list[subIdx], papers };
    return list;
   });
  } else {
   setSubjects((prev) => {
    const next = [...prev];
    const papers = [...next[subIdx].papers];
    const chapters = [...papers[papIdx].chapters];
    if (chapIdx >= chapters.length - 1) return prev;
    const temp = chapters[chapIdx + 1];
    chapters[chapIdx + 1] = chapters[chapIdx];
    chapters[chapIdx] = temp;
    papers[papIdx] = { ...papers[papIdx], chapters };
    next[subIdx] = { ...next[subIdx], papers };
    return next;
   });
  }
 };

 // Save Individual Day Edit for Student Routine
 const handleSaveStudentDayEdit = async (
  studentId: string, 
  updatedDays: RoutineItem[], 
  changedDaySummary: string,
  mode: 'Offline' | 'Online' = 'Offline'
 ) => {
  setIsSyncing(true);
  try {
   const studentKey = cleanStudentKey(studentId);
   const payload: any = {
    updatedEpoch: Date.now(),
    mode: mode
   };
   if (mode === 'Online') {
    payload.onlineRoutine = updatedDays;
    if (studentCustomRoutine) payload.offlineRoutine = studentCustomRoutine;
   } else {
    payload.offlineRoutine = updatedDays;
    payload.routineDays = updatedDays;
    if (studentCustomOnlineRoutine) payload.onlineRoutine = studentCustomOnlineRoutine;
   }

   const res = await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/custom_routine.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
   });

   if (res.ok) {
    if (mode === 'Online') {
     setStudentCustomOnlineRoutine(updatedDays);
    } else {
     setStudentCustomRoutine(updatedDays);
    }
    setEditingDayIndex(null);
    setEditingDayData(null);
    sound.playSuccess();
    showNotification(`Saved customized ${mode} routine for ${studentId}!`);

    // Send a smart notice to the student about the specific update
    const noticePayload = {
     title: 'Personal Study Schedule Updated',
     message: `Your mentor updated your ${mode} schedule: ${changedDaySummary}`,
     category: 'Routine',
     actionUrl: '',
     actionButtonText: 'View Routine',
     timestamp: Date.now()
    };
    await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/personal_notice.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
     method: 'PUT',
     headers: { 'Content-Type': 'application/json' },
     body: JSON.stringify(noticePayload)
    });
   }
  } catch (_e) {
   sound.playNegative();
   showNotification('Failed to save student custom routine', 'error');
  } finally {
   setIsSyncing(false);
  }
 };

 // Save Individual Exam Day Edit for Student Exams
 const handleSaveStudentExamDayEdit = async (
  studentId: string, 
  updatedExams: any[], 
  changedSummary: string,
  mode: 'Offline' | 'Online' = 'Offline'
 ) => {
  setIsSyncing(true);
  try {
   const studentKey = cleanStudentKey(studentId);
   const payload: any = {
    updatedEpoch: Date.now(),
    mode: mode
   };
   if (mode === 'Online') {
    payload.onlineExams = updatedExams;
    if (studentCustomOfflineExams) payload.offlineExams = studentCustomOfflineExams;
   } else {
    payload.offlineExams = updatedExams;
    payload.examList = updatedExams;
    if (studentCustomOnlineExams) payload.onlineExams = studentCustomOnlineExams;
   }

   const res = await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/custom_exams.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
   });

   if (res.ok) {
    if (mode === 'Online') {
     setStudentCustomOnlineExams(updatedExams);
    } else {
     setStudentCustomOfflineExams(updatedExams);
    }
    setEditingExamItem(null);
    sound.playSuccess();
    showNotification(`Saved customized ${mode} exam schedule for ${studentId}!`);

    const noticePayload = {
     title: 'Personal Exam Schedule Updated',
     message: `Your mentor updated your ${mode} exam schedule: ${changedSummary}`,
     category: 'Exam',
     actionUrl: '',
     actionButtonText: 'View Exams',
     timestamp: Date.now()
    };
    await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/personal_notice.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
     method: 'PUT',
     headers: { 'Content-Type': 'application/json' },
     body: JSON.stringify(noticePayload)
    });
   }
  } catch (_e) {
   sound.playNegative();
   showNotification('Failed to save student custom exams', 'error');
  } finally {
   setIsSyncing(false);
  }
 };

 // Apply Direct JSON String to Student
 const handleApplyDirectJsonToStudent = async (studentId: string, jsonString: string) => {
  try {
   const parsed = JSON.parse(jsonString);
   if (studentDetailSubTab === 'offlineRoutine' || studentDetailSubTab === 'onlineRoutine') {
    const isOnline = studentDetailSubTab === 'onlineRoutine';
    const days: RoutineItem[] = Array.isArray(parsed) ? parsed : (parsed.routineDays || (isOnline ? parsed.onlineRoutine : parsed.offlineRoutine) || []);
    if (!Array.isArray(days) || days.length === 0) {
     showNotification('Invalid JSON: Must be an array of routine days or have a "routineDays" array', 'error');
     return;
    }
    await handleSaveStudentDayEdit(studentId, days, `Assigned custom ${days.length}-day curriculum.`, isOnline ? 'Online' : 'Offline');
   } else {
    const isOnline = studentDetailSubTab === 'onlineExams';
    const exams: any[] = Array.isArray(parsed) ? parsed : (parsed.examList || (isOnline ? parsed.onlineExams : parsed.offlineExams) || []);
    if (!Array.isArray(exams) || exams.length === 0) {
     showNotification('Invalid JSON: Must be an array of exam items or have an "examList" array', 'error');
     return;
    }
    await handleSaveStudentExamDayEdit(studentId, exams, `Assigned custom ${exams.length} exam items.`, isOnline ? 'Online' : 'Offline');
   }
   setIsDirectJsonEditorOpen(false);
  } catch (e: any) {
   showNotification(`JSON Parse Error: ${e.message}`, 'error');
  }
 };

 // Add a new Day to Student Custom Routine
 const handleAddStudentDay = () => {
  if (!selectedStudentForDetail) return;
  const isOnline = studentDetailSubTab === 'onlineRoutine';
  const currentList = isOnline
   ? (studentCustomOnlineRoutine && studentCustomOnlineRoutine.length > 0 ? studentCustomOnlineRoutine : onlineRoutines)
   : (studentCustomRoutine && studentCustomRoutine.length > 0 ? studentCustomRoutine : offlineRoutines);

  setEditingDayIndex(currentList.length);
  setEditingDayData({
   dayNumber: currentList.length + 1,
   date: '',
   dayName: '',
   classSubject: '',
   classRoom: '',
   classTime: '',
   examDetails: '',
   isExamDay: false,
   examType: 'Daily MCQ',
   topics: ''
  });
 };

 // Delete a Day from Student Custom Routine
 const handleDeleteStudentDay = (idx: number) => {
  if (!selectedStudentForDetail) return;
  const isOnline = studentDetailSubTab === 'onlineRoutine';
  const currentList = isOnline
   ? (studentCustomOnlineRoutine && studentCustomOnlineRoutine.length > 0 ? [...studentCustomOnlineRoutine] : [...onlineRoutines])
   : (studentCustomRoutine && studentCustomRoutine.length > 0 ? [...studentCustomRoutine] : [...offlineRoutines]);

  if (idx < 0 || idx >= currentList.length) return;
  const itemDate = currentList[idx].date || `Day ${idx + 1}`;
  const dayNumber = (currentList[idx] as any).dayNumber || (idx + 1);

  showCustomConfirm(
   `Delete Day ${dayNumber} (${itemDate})?`,
   `Are you sure you want to remove this day from ${selectedStudentForDetail.name}'s custom ${isOnline ? 'Online' : 'Offline'} routine? This will immediately sync to the student's device.`,
   async () => {
    currentList.splice(idx, 1);
    await handleSaveStudentDayEdit(
     selectedStudentForDetail.studentId,
     currentList,
     `Removed Day (${itemDate}) from curriculum`,
     isOnline ? 'Online' : 'Offline'
    );
   },
   { confirmText: 'Delete Day', cancelText: 'Cancel', isDanger: true }
  );
 };

 // Delete an Exam from Student Custom Exams
 const handleDeleteStudentExam = (idx: number) => {
  if (!selectedStudentForDetail) return;
  const isOnline = studentDetailSubTab === 'onlineExams';
  const currentList = isOnline
   ? (studentCustomOnlineExams && studentCustomOnlineExams.length > 0 ? [...studentCustomOnlineExams] : [...onlineExams])
   : (studentCustomOfflineExams && studentCustomOfflineExams.length > 0 ? [...studentCustomOfflineExams] : [...offlineExams]);

  if (idx < 0 || idx >= currentList.length) return;
  const itemDate = currentList[idx].date || `Exam ${idx + 1}`;

  showCustomConfirm(
   `Delete Exam on ${itemDate}?`,
   `Are you sure you want to remove this exam from ${selectedStudentForDetail.name}'s schedule? This will immediately sync to the student's device.`,
   async () => {
    currentList.splice(idx, 1);
    await handleSaveStudentExamDayEdit(
     selectedStudentForDetail.studentId,
     currentList,
     `Removed Exam (${itemDate}) from schedule`,
     isOnline ? 'Online' : 'Offline'
    );
   },
   { confirmText: 'Delete Exam', cancelText: 'Cancel', isDanger: true }
  );
 };

 // Revert / Reset a Single Day back to Global Master Routine
 const handleResetSingleDayToGlobal = (dayIndex: number) => {
  if (!selectedStudentForDetail) return;
  const isOnline = studentDetailSubTab === 'onlineRoutine';
  const masterList = isOnline ? onlineRoutines : offlineRoutines;
  const currentList = isOnline
   ? (studentCustomOnlineRoutine && studentCustomOnlineRoutine.length > 0 ? [...studentCustomOnlineRoutine] : [...onlineRoutines])
   : (studentCustomRoutine && studentCustomRoutine.length > 0 ? [...studentCustomRoutine] : [...offlineRoutines]);

  const targetItem = currentList[dayIndex];
  if (!targetItem) return;

  const itemDate = targetItem.date || `Day ${dayIndex + 1}`;
  const masterMatch = masterList.find(g =>
   (g.date && targetItem.date && g.date.trim().toLowerCase() === targetItem.date.trim().toLowerCase()) ||
   (g.id && targetItem.id && g.id === targetItem.id)
  ) || masterList[dayIndex];

  if (!masterMatch) {
   showNotification('No matching Global Master day found for this date', 'error');
   return;
  }

  showCustomConfirm(
   `Revert Day ${(targetItem as any).dayNumber || (dayIndex + 1)} to Global?`,
   `This will restore the original master schedule for ${itemDate} (${masterMatch.classSubject || 'Self Study / Class'}) and sync live to ${selectedStudentForDetail.name}.`,
   async () => {
    currentList[dayIndex] = { ...masterMatch };
    const remainingCustom = currentList.filter(item => isRoutineItemCustomized(item, masterList)).length;

    if (remainingCustom === 0) {
     setIsSyncing(true);
     try {
      const studentKey = cleanStudentKey(selectedStudentForDetail.studentId);
      await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/custom_routine.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
       method: 'DELETE'
      });
      setStudentCustomRoutine(null);
      setStudentCustomOnlineRoutine(null);
      sound.playSuccess();
      showNotification(`All days in sync! Reverted routine to Global Master.`);
     } catch (_e) {
      sound.playNegative();
      showNotification('Failed to clear custom routine', 'error');
     } finally {
      setIsSyncing(false);
     }
    } else {
     await handleSaveStudentDayEdit(
      selectedStudentForDetail.studentId,
      currentList,
      `Reverted Day (${itemDate}) to Global Master`,
      isOnline ? 'Online' : 'Offline'
     );
     showNotification(` Day (${itemDate}) reverted to Global Master!`);
    }
   },
   { confirmText: 'Revert to Global', cancelText: 'Cancel', isDanger: false }
  );
 };

 // Revert / Reset a Single Exam back to Global Master Schedule
 const handleResetSingleExamToGlobal = (examIndex: number) => {
  if (!selectedStudentForDetail) return;
  const isOnline = studentDetailSubTab === 'onlineExams';
  const masterList = isOnline ? onlineExams : offlineExams;
  const currentList = isOnline
   ? (studentCustomOnlineExams && studentCustomOnlineExams.length > 0 ? [...studentCustomOnlineExams] : [...onlineExams])
   : (studentCustomOfflineExams && studentCustomOfflineExams.length > 0 ? [...studentCustomOfflineExams] : [...offlineExams]);

  const targetItem = currentList[examIndex];
  if (!targetItem) return;

  const itemDate = targetItem.date || `Exam ${examIndex + 1}`;
  const masterMatch = masterList.find(g =>
   (g.date && targetItem.date && g.date.trim().toLowerCase() === targetItem.date.trim().toLowerCase()) ||
   (g.id && targetItem.id && g.id === targetItem.id)
  ) || masterList[examIndex];

  if (!masterMatch) {
   showNotification('No matching Global Master exam found for this date', 'error');
   return;
  }

  showCustomConfirm(
   `Revert Exam on ${itemDate} to Global?`,
   `This will restore the original master exam assessment for ${itemDate} (${masterMatch.exams || masterMatch.examName || 'Assessment'}) and sync live to ${selectedStudentForDetail.name}.`,
   async () => {
    currentList[examIndex] = { ...masterMatch };
    const remainingCustom = currentList.filter(item => isExamItemCustomized(item, masterList)).length;

    if (remainingCustom === 0) {
     setIsSyncing(true);
     try {
      const studentKey = cleanStudentKey(selectedStudentForDetail.studentId);
      await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${studentKey}/custom_exams.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
       method: 'DELETE'
      });
      setStudentCustomOfflineExams(null);
      setStudentCustomOnlineExams(null);
      sound.playSuccess();
      showNotification(`All exams in sync! Reverted back to Global Master Exams.`);
     } catch (_e) {
      sound.playNegative();
      showNotification('Failed to clear custom exams', 'error');
     } finally {
      setIsSyncing(false);
     }
    } else {
     await handleSaveStudentExamDayEdit(
      selectedStudentForDetail.studentId,
      currentList,
      `Reverted Exam (${itemDate}) to Global Master`,
      isOnline ? 'Online' : 'Offline'
     );
     showNotification(` Exam on ${itemDate} reverted to Global Master!`);
    }
   },
   { confirmText: 'Revert to Global', cancelText: 'Cancel', isDanger: false }
  );
 };

 // Sort Student Schedule Chronologically
 const handleSortStudentSchedule = async () => {
  if (!selectedStudentForDetail) return;
  const parseDateEpoch = (d: string): number => {
   const months: Record<string, number> = {
    Jan: 0, Feb: 1, Mar: 2, Apr: 3, May: 4, Jun: 5,
    Jul: 6, Aug: 7, Sep: 8, Oct: 9, Nov: 10, Dec: 11
   };
   const m = (d || '').match(/^(\d{1,2})-([A-Za-z]{3})-(\d{2,4})$/);
   if (!m) return 0;
   const yr = m[3].length === 2 ? 2000 + Number(m[3]) : Number(m[3]);
   return new Date(yr, months[m[2]] ?? 0, Number(m[1])).getTime();
  };

  if (studentDetailSubTab === 'offlineRoutine' || studentDetailSubTab === 'onlineRoutine') {
   const isOnline = studentDetailSubTab === 'onlineRoutine';
   const currentList = isOnline
    ? (studentCustomOnlineRoutine && studentCustomOnlineRoutine.length > 0 ? [...studentCustomOnlineRoutine] : [...onlineRoutines])
    : (studentCustomRoutine && studentCustomRoutine.length > 0 ? [...studentCustomRoutine] : [...offlineRoutines]);
   currentList.sort((a, b) => parseDateEpoch(a.date) - parseDateEpoch(b.date));
   await handleSaveStudentDayEdit(selectedStudentForDetail.studentId, currentList, 'Sorted schedule by date', isOnline ? 'Online' : 'Offline');
  } else {
   const isOnline = studentDetailSubTab === 'onlineExams';
   const currentList = isOnline
    ? (studentCustomOnlineExams && studentCustomOnlineExams.length > 0 ? [...studentCustomOnlineExams] : [...onlineExams])
    : (studentCustomOfflineExams && studentCustomOfflineExams.length > 0 ? [...studentCustomOfflineExams] : [...offlineExams]);
   currentList.sort((a, b) => parseDateEpoch(a.date) - parseDateEpoch(b.date));
   await handleSaveStudentExamDayEdit(selectedStudentForDetail.studentId, currentList, 'Sorted exam schedule by date', isOnline ? 'Online' : 'Offline');
  }
 };

 // Push Active Student Custom Schedule Live
 const handlePushStudentScheduleLive = async () => {
  if (!selectedStudentForDetail) return;
  if (studentDetailSubTab === 'offlineRoutine' || studentDetailSubTab === 'onlineRoutine') {
   const isOnline = studentDetailSubTab === 'onlineRoutine';
   const currentList = isOnline
    ? (studentCustomOnlineRoutine && studentCustomOnlineRoutine.length > 0 ? studentCustomOnlineRoutine : onlineRoutines)
    : (studentCustomRoutine && studentCustomRoutine.length > 0 ? studentCustomRoutine : offlineRoutines);
   await handleSaveStudentDayEdit(
    selectedStudentForDetail.studentId,
    currentList,
    `Pushed ${isOnline ? 'Online' : 'Offline'} custom routine live (${currentList.length} Days)`,
    isOnline ? 'Online' : 'Offline'
   );
  } else {
   const isOnline = studentDetailSubTab === 'onlineExams';
   const currentList = isOnline
    ? (studentCustomOnlineExams && studentCustomOnlineExams.length > 0 ? studentCustomOnlineExams : onlineExams)
    : (studentCustomOfflineExams && studentCustomOfflineExams.length > 0 ? studentCustomOfflineExams : offlineExams);
   await handleSaveStudentExamDayEdit(
    selectedStudentForDetail.studentId,
    currentList,
    `Pushed ${isOnline ? 'Online' : 'Offline'} custom exams live (${currentList.length} Items)`,
    isOnline ? 'Online' : 'Offline'
   );
  }
 };

 // Export Student Active Schedule as JSON
 const handleExportStudentJSON = () => {
  if (!selectedStudentForDetail) return;
  let data: any = [];
  let filename = `eap_${selectedStudentForDetail.studentId}_${studentDetailSubTab}.json`;
  if (studentDetailSubTab === 'offlineRoutine') {
   data = (studentCustomRoutine && studentCustomRoutine.length > 0) ? studentCustomRoutine : offlineRoutines;
  } else if (studentDetailSubTab === 'onlineRoutine') {
   data = (studentCustomOnlineRoutine && studentCustomOnlineRoutine.length > 0) ? studentCustomOnlineRoutine : onlineRoutines;
  } else if (studentDetailSubTab === 'offlineExams') {
   data = (studentCustomOfflineExams && studentCustomOfflineExams.length > 0) ? studentCustomOfflineExams : offlineExams;
  } else {
   data = (studentCustomOnlineExams && studentCustomOnlineExams.length > 0) ? studentCustomOnlineExams : onlineExams;
  }
  downloadJSON(filename, data);
 };

 // Helper to safely convert any value (string, array, object) to a comparable trimmed string
 const normalizeComparableValue = (val: any): string => {
  if (val === null || val === undefined) return '';
  if (Array.isArray(val)) return val.map(v => String(v || '').trim()).filter(Boolean).join(' | ');
  if (typeof val === 'object') return JSON.stringify(val);
  return String(val).trim();
 };

 // Detect if a specific routine item has been customized compared to the global master
 const isRoutineItemCustomized = (item: any, globalList: RoutineItem[]): boolean => {
  if (!item) return false;
  const itemDate = normalizeComparableValue(item.date).toLowerCase();
  const globalMatch = globalList.find(g => {
   const gDate = normalizeComparableValue(g.date).toLowerCase();
   return (gDate && itemDate && gDate === itemDate) || (g.id && item.id && g.id === item.id);
  });
  if (!globalMatch) return true; // Brand new day added -> custom

  const subjectMatch = normalizeComparableValue(globalMatch.classSubject) === normalizeComparableValue(item.classSubject);
  const examMatch = normalizeComparableValue(globalMatch.examDetails) === normalizeComparableValue(item.examDetails);
  const topicsMatch = normalizeComparableValue(globalMatch.topics) === normalizeComparableValue(item.topics);
  const dayMatch = normalizeComparableValue(globalMatch.day) === normalizeComparableValue(item.day);

  return !(subjectMatch && examMatch && topicsMatch && dayMatch);
 };

 // Detect if a specific exam schedule item has been customized compared to the global master
 const isExamItemCustomized = (item: any, globalList: any[]): boolean => {
  if (!item) return false;
  const itemDate = normalizeComparableValue(item.date).toLowerCase();
  const globalMatch = globalList.find(g => {
   const gDate = normalizeComparableValue(g.date).toLowerCase();
   return (gDate && itemDate && gDate === itemDate) || (g.id && item.id && g.id === item.id);
  });
  if (!globalMatch) return true;

  const gName = normalizeComparableValue(globalMatch.exams || globalMatch.examName || (globalMatch as any).title);
  const iName = normalizeComparableValue(item.exams || item.examName || item.title);
  const nameMatch = gName === iName;

  const gSyllabus = normalizeComparableValue(globalMatch.syllabus || globalMatch.topics);
  const iSyllabus = normalizeComparableValue(item.syllabus || item.topics);
  const syllabusMatch = gSyllabus === iSyllabus;

  const gDay = normalizeComparableValue(globalMatch.day || (globalMatch as any).dayName);
  const iDay = normalizeComparableValue(item.day || item.dayName);
  const dayMatch = gDay === iDay;

  return !(nameMatch && syllabusMatch && dayMatch);
 };

 // Push Single Day Routine Live to Specific Student (RTDB + Real-time FCM)
 const handlePushSingleDayToStudent = async (studentId: string, item: RoutineItem, mode: 'Offline' | 'Online') => {
  if (!studentId || !item) return;
  setIsSyncing(true);
  try {
   const isOnline = mode === 'Online';
   const currentList = isOnline
    ? (studentCustomOnlineRoutine && studentCustomOnlineRoutine.length > 0 ? [...studentCustomOnlineRoutine] : [...onlineRoutines])
    : (studentCustomRoutine && studentCustomRoutine.length > 0 ? [...studentCustomRoutine] : [...offlineRoutines]);

   await handleSaveStudentDayEdit(
    studentId,
    currentList,
    `Pushed Day update for ${item.date} (${item.classSubject || 'Class'})`,
    mode
   );
   showNotification(` Live push sent for ${item.date} to student!`);
  } catch (err: any) {
   showNotification(`Failed to push day: ${err.message}`, 'error');
  } finally {
   setIsSyncing(false);
  }
 };

 // Push Single Exam Schedule Live to Specific Student (RTDB + Real-time FCM)
 const handlePushSingleExamToStudent = async (studentId: string, item: any, mode: 'Offline' | 'Online') => {
  if (!studentId || !item) return;
  setIsSyncing(true);
  try {
   const isOnline = mode === 'Online';
   const currentList = isOnline
    ? (studentCustomOnlineExams && studentCustomOnlineExams.length > 0 ? [...studentCustomOnlineExams] : [...onlineExams])
    : (studentCustomOfflineExams && studentCustomOfflineExams.length > 0 ? [...studentCustomOfflineExams] : [...offlineExams]);

   await handleSaveStudentExamDayEdit(
    studentId,
    currentList,
    `Pushed Exam update for ${item.date} (${item.exams || item.examName || 'Assessment'})`,
    mode
   );
   showNotification(` Live push sent for exam on ${item.date} to student!`);
  } catch (err: any) {
   showNotification(`Failed to push exam: ${err.message}`, 'error');
  } finally {
   setIsSyncing(false);
  }
 };
 useEffect(() => {
  if (isAuthenticated) {
   fetchStudents();
   fetchActiveTargetExam();
   fetchSentNotifications();
  }
 }, [isAuthenticated, activeTab]);

 // Handle Preset Selection Change
 const handleTargetExamPresetChange = (preset: string) => {
  setTargetExamPreset(preset);
  if (preset !== 'Custom') {
   const match = admissionExamPresets.find(p => p.name.toLowerCase().includes(preset.toLowerCase()) || preset.toLowerCase().includes(p.name.toLowerCase()));
   if (match) {
    setTargetExamDays(match.days);
    setTargetExamDateInput(calculateDefaultExamDate(match.days));
   }
  }
 };

 // Handle Calendar Date Picker Change for Target Exam
 const handleTargetExamDateChange = (dateStr: string) => {
  setTargetExamDateInput(dateStr);
  if (dateStr) {
   const targetTime = new Date(dateStr + 'T00:00:00').getTime();
   const now = new Date().setHours(0, 0, 0, 0);
   const diffDays = Math.ceil((targetTime - now) / (1000 * 60 * 60 * 24));
   setTargetExamDays(Math.max(1, diffDays));
  }
 };

 // Clear / Reset Target Exam Countdown on Student Devices
 const handleClearTargetExam = async () => {
  showCustomConfirm(
   'Clear Target Countdown on Student Devices?',
   'This will reset the active milestone countdown on all student mobile apps back to default.',
   async () => {
    setIsSyncing(true);
    try {
     let baseUrl = apiEndpoint.trim().replace(/\/eap_data\.json.*$/, '').replace(/\/+$/, '');
     if (!baseUrl.includes('firebaseio.com')) {
      baseUrl = 'https://eap-tracker-default-rtdb.firebaseio.com';
     }
     const targetUrl = `${baseUrl}/target_exam_config.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`;

     await fetch(targetUrl, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ cleared: true, timestamp: Date.now() })
     });

     await dispatchFCMTopicPush(
      'Target Milestone Reset',
      'Official target countdown has been reset by mentor.',
      'eap_general',
      { category: 'TargetExam', type: 'countdown_reset', cleared: 'true' }
     );

     sound.playSuccess();
     showNotification('Target Exam Countdown cleared on all student devices!');
    } catch (_e) {
     sound.playNegative();
     showNotification('Failed to clear target countdown', 'error');
    } finally {
     setIsSyncing(false);
    }
   },
   { confirmText: 'Clear Countdown', cancelText: 'Cancel', isDanger: true }
  );
 };

 // Upload and Parse Routine JSON
 const handleRoutineJsonFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
  const file = e.target.files?.[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = (event) => {
   try {
    const content = event.target?.result as string;
    const parsed = JSON.parse(content);

    let extractedRoutines: RoutineItem[] = [];
    if (Array.isArray(parsed)) {
     extractedRoutines = parsed;
    } else if (parsed.offlineRoutine && Array.isArray(parsed.offlineRoutine)) {
     extractedRoutines = parsed.offlineRoutine;
    } else if (parsed.onlineRoutine && Array.isArray(parsed.onlineRoutine)) {
     extractedRoutines = parsed.onlineRoutine;
    } else if (parsed.routines && Array.isArray(parsed.routines)) {
     extractedRoutines = parsed.routines;
    } else {
     throw new Error('Unrecognized JSON format. Expected an array of routine items or { offlineRoutine: [...] }');
    }

    setUploadedRoutinePreview(extractedRoutines);
    setUploadedRoutineFileName(file.name);
    if (uploadedRoutineTargetMode !== 'Individual') {
     setUploadedRoutineTargetMode(routineMode);
    }
    setIsUploadingRoutineModalOpen(true);
    sound.playSuccess();
    showNotification(`Parsed ${extractedRoutines.length} routine days from ${file.name}!`);
   } catch (err: any) {
    sound.playNegative();
    showNotification(`JSON Parse Error: ${err.message}`, 'error');
   }
  };
  reader.readAsText(file);
  e.target.value = '';
 };

 // Apply or Push Uploaded Routine
 const handleApplyUploadedRoutine = async () => {
  if (!uploadedRoutinePreview || uploadedRoutinePreview.length === 0) return;

  setIsSyncing(true);
  try {
   if (uploadedRoutineTargetMode === 'Individual') {
    if (!uploadedRoutineTargetStudentId.trim()) {
     showNotification('Please select or enter a Student ID (e.g. EAP-2025-042)', 'error');
     setIsSyncing(false);
     return;
    }
    const studentKey = cleanStudentKey(uploadedRoutineTargetStudentId);
    let baseUrl = apiEndpoint.trim().replace(/\/eap_data\.json.*$/, '').replace(/\/+$/, '');
    if (!baseUrl.includes('firebaseio.com')) {
     baseUrl = 'https://eap-tracker-default-rtdb.firebaseio.com';
    }
    const targetUrl = `${baseUrl}/students/${studentKey}/custom_routine.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`;

    const isOnlineTarget = studentDetailSubTab === 'onlineRoutine';
    const res = await fetch(targetUrl, {
     method: 'PATCH',
     headers: { 'Content-Type': 'application/json' },
     body: JSON.stringify({
      updatedEpoch: Date.now(),
      mode: isOnlineTarget ? 'Online' : 'Offline',
      offlineRoutine: isOnlineTarget ? (studentCustomRoutine || offlineRoutines) : uploadedRoutinePreview,
      onlineRoutine: isOnlineTarget ? uploadedRoutinePreview : (studentCustomOnlineRoutine || onlineRoutines),
      routineDays: uploadedRoutinePreview
     })
    });

    if (res.ok) {
     if (isOnlineTarget) {
      setStudentCustomOnlineRoutine(uploadedRoutinePreview);
     } else {
      setStudentCustomRoutine(uploadedRoutinePreview);
     }
     sound.playSuccess();
     showNotification(`Custom routine pushed directly to Student ID ${uploadedRoutineTargetStudentId}!`);
     setIsUploadingRoutineModalOpen(false);
     setUploadedRoutinePreview(null);
    } else {
     throw new Error(`HTTP ${res.status}`);
    }
   } else {
    if (uploadedRoutineTargetMode === 'Offline') {
     setOfflineRoutines(uploadedRoutinePreview);
    } else {
     setOnlineRoutines(uploadedRoutinePreview);
    }
    sound.playSuccess();
    showNotification(`Applied ${uploadedRoutinePreview.length} days to ${uploadedRoutineTargetMode} routine! Ready to publish to cloud.`);
    setIsUploadingRoutineModalOpen(false);
    setUploadedRoutinePreview(null);
   }
  } catch (err: any) {
   sound.playNegative();
   showNotification(`Push error: ${err.message}`, 'error');
  } finally {
   setIsSyncing(false);
  }
 };

 // Send Personal Notice to Specific Student (RTDB + Real-time FCM Token Push)
 const handleSendPersonalNotice = async () => {
  if (!personalNoticeStudentId.trim() || !personalNoticeMessage.trim()) {
   showNotification('Student ID and Message are required', 'error');
   return;
  }

  setIsSyncing(true);
  try {
   const studentKey = cleanStudentKey(personalNoticeStudentId);
   let baseUrl = apiEndpoint.trim().replace(/\/eap_data\.json.*$/, '').replace(/\/+$/, '');
   if (!baseUrl.includes('firebaseio.com')) {
    baseUrl = 'https://eap-tracker-default-rtdb.firebaseio.com';
   }
   const targetUrl = `${baseUrl}/students/${studentKey}/personal_notice.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`;

   const payload = {
    title: personalNoticeTitle.trim() || 'Mentor Notice',
    message: personalNoticeMessage.trim(),
    actionUrl: personalNoticeUrl.trim() || undefined,
    actionButtonText: personalNoticeButtonText.trim() || undefined,
    timestamp: Date.now()
   };

   // 1. Write persistent personal notice to RTDB
   const res = await fetch(targetUrl, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
   });

   if (!res.ok) {
    throw new Error(`RTDB Error HTTP ${res.status}`);
   }

   // 2. Look up student's registered FCM device token for background/killed delivery
   let studentFcmToken: string | null = null;
   try {
    const tokenRes = await fetch(`${baseUrl}/students/${studentKey}/fcm_token.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`);
    if (tokenRes.ok) {
     const rawToken = await tokenRes.json();
     if (typeof rawToken === 'string' && rawToken.length > 20) {
      studentFcmToken = rawToken;
     }
    }
    if (!studentFcmToken) {
     const profileTokenRes = await fetch(`${baseUrl}/students/${studentKey}/profile_summary/fcm_token.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`);
     if (profileTokenRes.ok) {
      const rawToken = await profileTokenRes.json();
      if (typeof rawToken === 'string' && rawToken.length > 20) {
       studentFcmToken = rawToken;
      }
     }
    }
   } catch (_e) {}

   // 3. Dispatch direct individual FCM Push to device via Cloudflare Worker
   let fcmDelivered = false;
   const targetWorkerUrl = workerUrl.trim();
   const targetWorkerSecret = workerSecret.trim();

   if (studentFcmToken && targetWorkerUrl && targetWorkerSecret) {
    try {
     const fcmRes = await fetch(targetWorkerUrl, {
      method: 'POST',
      headers: {
       'Content-Type': 'application/json',
       'Authorization': `Bearer ${targetWorkerSecret}`
      },
      body: JSON.stringify({
       token: studentFcmToken,
       title: payload.title,
       message: payload.message,
       data: {
        category: 'Personal',
        actionUrl: payload.actionUrl || '',
        actionButtonText: payload.actionButtonText || '',
        timestamp: payload.timestamp.toString()
       }
      })
     });
     if (fcmRes.ok) {
      fcmDelivered = true;
     }
    } catch (_fcmErr) {
     console.warn('Individual FCM push failed:', _fcmErr);
    }
   }

   sound.playSuccess();
   if (fcmDelivered) {
    showNotification(` Live FCM Push + RTDB notice delivered to ${personalNoticeStudentName || personalNoticeStudentId}!`);
   } else if (studentFcmToken) {
    showNotification(`Notice saved to RTDB for ${personalNoticeStudentName || personalNoticeStudentId}!`);
   } else {
    showNotification(`Notice saved to RTDB for ${personalNoticeStudentName || personalNoticeStudentId} (Device will sync on next open)!`);
   }

   setIsPersonalNoticeModalOpen(false);
   setPersonalNoticeMessage('');
   setPersonalNoticeUrl('');
  } catch (err: any) {
   sound.playNegative();
   showNotification(`Failed to send notice: ${err.message}`, 'error');
  } finally {
   setIsSyncing(false);
  }
 };

 // Generic JSON Downloader Helper
 const downloadJSON = (filename: string, data: any) => {
  try {
   const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(data, null, 2));
   const downloadAnchor = document.createElement('a');
   downloadAnchor.setAttribute("href", dataStr);
   downloadAnchor.setAttribute("download", filename);
   document.body.appendChild(downloadAnchor);
   downloadAnchor.click();
   downloadAnchor.remove();
   sound.playSuccess();
   showNotification(`Downloaded: ${filename}`);
  } catch (err: any) {
   sound.playNegative();
   showNotification(`Export error: ${err.message}`, 'error');
  }
 };

 // 1. Export Full Master Bundle (All Routines, Exams, Subjects)
 const handleExportFullMasterJSON = () => {
  const payload = generatePayload();
  const filename = `eap_full_master_schedule_${new Date().toISOString().slice(0, 10)}.json`;
  downloadJSON(filename, payload);
 };

 // 2. Export Offline Class Routine Only
 const handleExportOfflineRoutineJSON = () => {
  const filename = `eap_offline_class_routine_${new Date().toISOString().slice(0, 10)}.json`;
  downloadJSON(filename, {
   exportType: "offline_routine",
   lastUpdated: new Date().toISOString(),
   offlineRoutine: offlineRoutines
  });
 };

 // 3. Export Online Class Routine Only
 const handleExportOnlineRoutineJSON = () => {
  const filename = `eap_online_class_routine_${new Date().toISOString().slice(0, 10)}.json`;
  downloadJSON(filename, {
   exportType: "online_routine",
   lastUpdated: new Date().toISOString(),
   onlineRoutine: onlineRoutines
  });
 };

 // 4. Export Both Class Routines (Offline + Online)
 const handleExportAllRoutinesJSON = () => {
  const filename = `eap_all_class_routines_${new Date().toISOString().slice(0, 10)}.json`;
  downloadJSON(filename, {
   exportType: "all_routines",
   lastUpdated: new Date().toISOString(),
   offlineRoutine: offlineRoutines,
   onlineRoutine: onlineRoutines
  });
 };

 // 5. Export Offline Exam Schedule Only
 const handleExportOfflineExamsJSON = () => {
  const filename = `eap_offline_exam_schedule_${new Date().toISOString().slice(0, 10)}.json`;
  downloadJSON(filename, {
   exportType: "offline_exams",
   lastUpdated: new Date().toISOString(),
   offlineExams: offlineExams
  });
 };

 // 6. Export Online Exam Schedule Only
 const handleExportOnlineExamsJSON = () => {
  const filename = `eap_online_exam_schedule_${new Date().toISOString().slice(0, 10)}.json`;
  downloadJSON(filename, {
   exportType: "online_exams",
   lastUpdated: new Date().toISOString(),
   onlineExams: onlineExams
  });
 };

 // 7. Export Both Exam Schedules (Offline + Online)
 const handleExportAllExamsJSON = () => {
  const filename = `eap_all_exam_schedules_${new Date().toISOString().slice(0, 10)}.json`;
  downloadJSON(filename, {
   exportType: "all_exams",
   lastUpdated: new Date().toISOString(),
   offlineExams: offlineExams,
   onlineExams: onlineExams
  });
 };

 // Legacy alias for top buttons
 const handleExportJSON = handleExportFullMasterJSON;

 // Universal Smart JSON Importer
 const handleImportJSON = (e: React.ChangeEvent<HTMLInputElement>) => {
  const file = e.target.files?.[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = (event) => {
   try {
    const content = event.target?.result as string;
    const parsed = JSON.parse(content);
    const loadedModules: string[] = [];

    if (parsed.offlineRoutine && Array.isArray(parsed.offlineRoutine)) {
     setOfflineRoutines(parsed.offlineRoutine);
     loadedModules.push(`Offline Routine (${parsed.offlineRoutine.length} days)`);
    }
    if (parsed.onlineRoutine && Array.isArray(parsed.onlineRoutine)) {
     setOnlineRoutines(parsed.onlineRoutine);
     loadedModules.push(`Online Routine (${parsed.onlineRoutine.length} days)`);
    }
    if (parsed.offlineExams && Array.isArray(parsed.offlineExams)) {
     setOfflineExams(parsed.offlineExams);
     loadedModules.push(`Offline Exams (${parsed.offlineExams.length} items)`);
    }
    if (parsed.onlineExams && Array.isArray(parsed.onlineExams)) {
     setOnlineExams(parsed.onlineExams);
     loadedModules.push(`Online Exams (${parsed.onlineExams.length} items)`);
    }
    if (parsed.subjects && Array.isArray(parsed.subjects)) {
     setSubjects(parsed.subjects);
     loadedModules.push(`Subjects (${parsed.subjects.length})`);
    }
    if (parsed.updateMessage) {
     setUpdateMessage(parsed.updateMessage);
    }

    // Handle raw JSON Array format (routine or exam array)
    if (Array.isArray(parsed) && parsed.length > 0) {
     const first = parsed[0];
     if (first.classSubject || first.topics) {
      // It's a Routine Array
      if (routineMode === 'Offline') {
       setOfflineRoutines(parsed);
       loadedModules.push(`Offline Routine (${parsed.length} days)`);
      } else {
       setOnlineRoutines(parsed);
       loadedModules.push(`Online Routine (${parsed.length} days)`);
      }
     } else if (first.exams) {
      // It's an Exam Schedule Array
      if (examMode === 'Offline') {
       setOfflineExams(parsed);
       loadedModules.push(`Offline Exams (${parsed.length} items)`);
      } else {
       setOnlineExams(parsed);
       loadedModules.push(`Online Exams (${parsed.length} items)`);
      }
     }
    }

    if (loadedModules.length > 0) {
     sound.playSuccess();
     showNotification(`Restored ${file.name}: ${loadedModules.join(', ')}`);
    } else {
     sound.playNegative();
     showNotification('Unrecognized JSON format: no valid routine or exam datasets found', 'error');
    }
   } catch (err: any) {
    sound.playNegative();
    showNotification(`Failed to parse JSON file: ${err.message}`, 'error');
   }
  };
  reader.readAsText(file);
  e.target.value = '';
 };

 // Reset to Official Master Schedule
 const handleResetToMasterSchedule = () => {
  showCustomConfirm(
   'Reset Schedules to Official 2026 Master?',
   'Reset all routines and exams to the official EAP Admission 2026 curriculum schedule?',
   () => {
    if (masterEapData.offlineRoutine) setOfflineRoutines(masterEapData.offlineRoutine as any);
    if (masterEapData.onlineRoutine) setOnlineRoutines(masterEapData.onlineRoutine as any);
    if (masterEapData.offlineExams) setOfflineExams(masterEapData.offlineExams as any);
    if (masterEapData.onlineExams) setOnlineExams(masterEapData.onlineExams as any);
    if (masterEapData.updateMessage) setUpdateMessage(masterEapData.updateMessage);
    sound.playSuccess();
    showNotification('Reset to official EAP 2026 routine & exam schedules!');
   },
   { confirmText: 'Reset Schedules', cancelText: 'Cancel', isDanger: true }
  );
 };

 // Generate Master Payload
 const generatePayload = () => {
  return {
   configVersion: Date.now(),
   lastUpdated: new Date().toISOString(),
   updateMessage: updateMessage.trim() || 'New Routine & Exam Schedule Published! Tap to view your latest timetable.',
   offlineRoutine: offlineRoutines,
   onlineRoutine: onlineRoutines,
   offlineExams: offlineExams,
   onlineExams: onlineExams,
   subjects: subjects
  };
 };

 // Modular Live Cloud Publisher (supports: master, offlineRoutine, onlineRoutine, allRoutines, offlineExams, onlineExams, allExams, subjects, singleDayRoutine, singleDayExam)
 const handlePublishModule = async (
  target: 'master' | 'offlineRoutine' | 'onlineRoutine' | 'allRoutines' | 'offlineExams' | 'onlineExams' | 'allExams' | 'subjects' | 'singleDayRoutine' | 'singleDayExam',
  extraData?: any,
  customMsg?: string
 ) => {
  let pushSuccess = true;
  if (!apiEndpoint.trim()) {
   showNotification('Please provide a REST API Endpoint URL', 'error');
   return;
  }

  setIsSyncing(true);
  try {
   let targetUrl = apiEndpoint.trim();
   const isFirebase = targetUrl.includes('firebaseio.com');

   if (isFirebase) {
    if (!targetUrl.endsWith('.json') && !targetUrl.includes('.json?')) {
     targetUrl = targetUrl.replace(/\/+$/, '') + '/eap_data.json';
    }
    if (apiKey.trim() && !targetUrl.includes('auth=')) {
     targetUrl += (targetUrl.includes('?') ? '&' : '?') + `auth=${apiKey.trim()}`;
    }
   }

   let payload: any = {
    configVersion: Date.now(),
    lastUpdated: new Date().toISOString()
   };

   let smartTitle = 'EAP Tracker Update';
   let smartMsg = 'Schedule update received.';
   let smartCategory = 'Notice';
   let smartRoute = 'home';

   switch (target) {
    case 'master':
     payload = generatePayload();
     smartTitle = 'Full Admission Schedule Updated';
     smartMsg = customMsg || updateMessage.trim() || 'Complete class routines, exam schedules, and syllabus chapters have been synchronized.';
     smartCategory = 'Notice';
     smartRoute = 'home';
     break;
    case 'offlineRoutine':
     payload.offlineRoutine = offlineRoutines;
     smartTitle = 'Offline Class Routine Updated';
     smartMsg = customMsg || updateMessage.trim() || `Offline class routine updated with ${offlineRoutines.length} days scheduled. Tap to check your timetable.`;
     smartCategory = 'Schedule';
     smartRoute = 'routine';
     break;
    case 'onlineRoutine':
     payload.onlineRoutine = onlineRoutines;
     smartTitle = 'Online Class Routine Updated';
     smartMsg = customMsg || updateMessage.trim() || `Online live class routine updated with ${onlineRoutines.length} days scheduled. Tap to check your timetable.`;
     smartCategory = 'Schedule';
     smartRoute = 'routine';
     break;
    case 'allRoutines':
     payload.offlineRoutine = offlineRoutines;
     payload.onlineRoutine = onlineRoutines;
     smartTitle = 'Class Routines Synchronized';
     smartMsg = customMsg || updateMessage.trim() || `Both Offline (${offlineRoutines.length}d) and Online (${onlineRoutines.length}d) class routines updated.`;
     smartCategory = 'Schedule';
     smartRoute = 'routine';
     break;
    case 'offlineExams':
     payload.offlineExams = offlineExams;
     smartTitle = 'Offline Exam Schedule Updated';
     smartMsg = customMsg || updateMessage.trim() || `Offline exam routine updated with ${offlineExams.length} tests scheduled. Check your upcoming exams.`;
     smartCategory = 'Exam';
     smartRoute = 'exam';
     break;
    case 'onlineExams':
     payload.onlineExams = onlineExams;
     smartTitle = 'Online Exam Schedule Updated';
     smartMsg = customMsg || updateMessage.trim() || `Online exam routine updated with ${onlineExams.length} tests scheduled. Check your upcoming exams.`;
     smartCategory = 'Exam';
     smartRoute = 'exam';
     break;
    case 'allExams':
     payload.offlineExams = offlineExams;
     payload.onlineExams = onlineExams;
     smartTitle = 'All Exam Schedules Synchronized';
     smartMsg = customMsg || updateMessage.trim() || `Both Offline (${offlineExams.length}) and Online (${onlineExams.length}) exam schedules updated.`;
     smartCategory = 'Exam';
     smartRoute = 'exam';
     break;
    case 'subjects':
     payload.subjects = subjects;
     {
      const subNames = subjects.map(s => s.name).join(', ');
      smartTitle = 'Syllabus Chapters Updated';
      smartMsg = customMsg || updateMessage.trim() || `Curriculum updated: ${subNames || 'Subject'} chapters and checklist synchronized.`;
      smartCategory = 'Syllabus';
      smartRoute = 'syllabus';
     }
     break;
    case 'singleDayRoutine':
     payload.singleDayRoutine = extraData;
     if (extraData?.type === 'Online') {
      payload.onlineRoutine = onlineRoutines;
     } else {
      payload.offlineRoutine = offlineRoutines;
     }
     {
      const it = extraData?.item;
      const dayDate = it?.date ? `${it.date} (${it.day || ''})` : 'Class Day';
      const subj = it?.classSubject || 'Class';
      const time = it?.classTime ? ` at ${it.classTime}` : '';
      const room = it?.classRoom ? ` in ${it.classRoom}` : '';
      const exam = it?.examDetails ? ` • Exam: ${it.examDetails}` : '';
      smartTitle = `Class Rescheduled: ${subj}`;
      smartMsg = customMsg || `${dayDate}: ${subj}${time}${room}${exam}. Tap to view updated schedule.`;
      smartCategory = 'Schedule';
      smartRoute = 'routine';
     }
     break;
    case 'singleDayExam':
     payload.singleDayExam = extraData;
     if (extraData?.type === 'Online') {
      payload.onlineExams = onlineExams;
     } else {
      payload.offlineExams = offlineExams;
     }
     {
      const it = extraData?.item;
      const examDate = it?.date ? `${it.date} (${it.day || ''})` : 'Exam Date';
      const examNames = Array.isArray(it?.exams) ? it.exams.join(', ') : (it?.exams || 'Exam Test');
      const time = it?.time ? ` at ${it.time}` : '';
      const type = it?.type ? ` [${it.type}]` : '';
      smartTitle = `Exam Schedule Update: ${it?.date || 'Upcoming Exam'}`;
      smartMsg = customMsg || `${examDate}: ${examNames}${type}${time}. Check your upcoming exam dates.`;
      smartCategory = 'Exam';
      smartRoute = 'exam';
     }
     break;
   }

   payload.updateTitle = smartTitle;
   payload.updateMessage = smartMsg;
   payload.updateCategory = smartCategory;
   payload.updateRoute = smartRoute;

   const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
   };

   if (!isFirebase && apiKey.trim()) {
    headers['Authorization'] = `Bearer ${apiKey.trim()}`;
    headers['X-Master-Key'] = apiKey.trim();
    headers['X-Api-Key'] = apiKey.trim();
   }

   // For Firebase RTDB, PATCH merges only the specified keys without overwriting others
   const method = isFirebase ? (target === 'master' ? 'PUT' : 'PATCH') : 'PUT';

   const res = await fetch(targetUrl, {
    method,
    headers,
    body: JSON.stringify(payload)
   });

   // Also dispatch instant broadcast notification to /broadcast_notification.json for active listeners
   if (isFirebase && res.ok) {
    try {
     const broadcastUrl = targetUrl.replace(/\/eap_data\.json(\?.*)?$/, '/broadcast_notification.json') + (apiKey.trim() ? `?auth=${apiKey.trim()}` : '');
     await fetch(broadcastUrl, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
       id: 'alert_' + Date.now(),
       title: smartTitle,
       message: smartMsg,
       category: smartCategory,
       timestamp: Date.now(),
       targetAudience: 'All',
       actionRoute: smartRoute
      })
     });

     // Also trigger instant FCM push for closed Android apps
     pushSuccess = await dispatchFCMTopicPush(smartTitle, smartMsg, 'eap_updates', {
      category: smartCategory,
      actionRoute: smartRoute
     });
    } catch (bErr) {
     console.warn('Broadcast notification sync warning:', bErr);
    }
   }

   if (res.ok) {
    const timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', month: 'short', day: 'numeric' });
    setLastSyncTime(timeStr);
    localStorage.setItem('eap_last_sync_time', timeStr);
    localStorage.setItem('eap_rest_endpoint', apiEndpoint.trim());
    localStorage.setItem('eap_rest_api_key', apiKey.trim());
    const readableTarget = target === 'master' ? 'Complete Master Bundle' : 
     target === 'offlineRoutine' ? 'Offline Routine Only' :
     target === 'onlineRoutine' ? 'Online Routine Only' :
     target === 'allRoutines' ? 'Both Class Routines' :
     target === 'offlineExams' ? 'Offline Exams Only' :
     target === 'onlineExams' ? 'Online Exams Only' :
     target === 'allExams' ? 'Both Exam Schedules' :
     target === 'singleDayRoutine' ? `Single Day (${extraData?.item?.date})` : `Single Exam (${extraData?.item?.date})`;
    
    if (pushSuccess) {
     sound.playSuccess();
     showNotification(`Live Cloud Update Successful: ${readableTarget}!`);
    } else {
     sound.playNegative();
     showNotification(`Data published successfully, but notification sending failed.`, 'error');
    }
   } else {
    const errText = await res.text();
    sound.playNegative();
    showNotification(`Server returned HTTP ${res.status}: ${errText.slice(0, 80)}`, 'error');
   }
  } catch (err: any) {
   sound.playNegative();
   showNotification(`Connection Error: ${err.message}`, 'error');
  } finally {
   setIsSyncing(false);
  }
 };

 // Full Cloud Publish Alias
 const handlePublishToCloud = () => handlePublishModule('master');

 /**
  * Direct Firebase push for delete operations — bypasses React state closure issues.
  * Call this immediately after computing the updated array on delete.
  * Also fires broadcast_notification so the user app gets an instant notification.
  */
 const pushDeletedArrayToFirebase = async (
  key: 'offlineRoutine' | 'onlineRoutine' | 'offlineExams' | 'onlineExams',
  updatedArray: any[]
 ) => {
  const endpoint = apiEndpoint.trim();
  if (!endpoint) return;
  const nowTs = Date.now();
  const isRoutine = key.includes('Routine');
  const notifTitle = isRoutine ? 'Class Routine Updated' : 'Exam Schedule Updated';
  const notifMsg = `An entry was removed from the ${isRoutine ? 'class routine' : 'exam schedule'}. Tap to view the updated schedule.`;
  const notifRoute = isRoutine ? 'routine' : 'exam';
  try {
   let baseUrl = endpoint;
   if (baseUrl.includes('firebaseio.com')) {
    baseUrl = baseUrl.replace(/\/eap_data\.json.*$/, '').replace(/\/+$/, '');
   }
   const authSuffix = apiKey.trim() ? `?auth=${apiKey.trim()}` : '';
   const eapUrl = `${baseUrl}/eap_data.json${authSuffix}`;
   const broadcastUrl = `${baseUrl}/broadcast_notification.json${authSuffix}`;

   const eapPayload = {
    configVersion: nowTs,
    lastUpdated: new Date().toISOString(),
    [key]: updatedArray,
    updateTitle: notifTitle,
    updateMessage: notifMsg,
    updateCategory: isRoutine ? 'Schedule' : 'Exam',
    updateRoute: notifRoute
   };

   // Step 1: PATCH eap_data so app syncs the deleted array
   await fetch(eapUrl, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(eapPayload)
   });

   // Step 2: PUT broadcast_notification so app shows ONE notification (dedup key = nowTs)
   await fetch(broadcastUrl, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
     id: `del_${nowTs}`,
     title: notifTitle,
     message: notifMsg,
     category: isRoutine ? 'Schedule' : 'Exam',
     timestamp: nowTs,
     targetAudience: 'All',
     actionRoute: notifRoute
    })
   });

   // Step 3: Trigger instant FCM push for closed Android apps
   const pushSuccess = await dispatchFCMTopicPush(notifTitle, notifMsg, 'eap_updates', {
    category: isRoutine ? 'Schedule' : 'Exam',
    actionRoute: notifRoute
   });

   if (pushSuccess) {
    sound.playSuccess();
    showNotification(`Array synced and Push Notification dispatched!`);
   } else {
    sound.playNegative();
    showNotification(`Data published successfully, but notification sending failed.`, 'error');
   }
  } catch (e) {
   console.warn('Delete sync warning:', e);
  }
 };

 // Cloud Fetch
 const handleFetchFromCloud = async () => {
  if (!apiEndpoint.trim()) {
   showNotification('Please provide a REST API Endpoint URL', 'error');
   return;
  }

  setIsSyncing(true);
  try {
   const headers: Record<string, string> = {
    'Accept': 'application/json'
   };
   if (apiKey.trim()) {
    headers['Authorization'] = `Bearer ${apiKey.trim()}`;
    headers['X-Master-Key'] = apiKey.trim();
    headers['X-Api-Key'] = apiKey.trim();
   }

   const res = await fetch(apiEndpoint.trim(), { method: 'GET', headers });
   if (res.ok) {
    const data = await res.json();
    const payload = data.record || data;

    if (payload.offlineRoutine) setOfflineRoutines(payload.offlineRoutine);
    if (payload.onlineRoutine) setOnlineRoutines(payload.onlineRoutine);
    if (payload.offlineExams) setOfflineExams(payload.offlineExams);
    if (payload.onlineExams) setOnlineExams(payload.onlineExams);
    if (payload.subjects) setSubjects(payload.subjects);

    const timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', month: 'short', day: 'numeric' });
    setLastSyncTime(timeStr);
    localStorage.setItem('eap_last_sync_time', timeStr);
    sound.playSuccess();
    showNotification('Fetched and synced latest cloud payload!');
   } else {
    sound.playNegative();
    showNotification(`Failed to fetch: HTTP ${res.status}`, 'error');
   }
  } catch (err: any) {
   sound.playNegative();
   showNotification(`Fetch Error: ${err.message}`, 'error');
  } finally {
   setIsSyncing(false);
  }
 };

 // Standalone Direct Instant Push Broadcast
 const handleSendDirectNotification = async (title?: string, body?: string, category?: string) => {
  const finalTitle = (title || directNotifTitle).trim() || 'EAP Notification';
  const finalBody = (body || directNotifBody).trim();
  const finalCat = category || directNotifCategory;

  if (!finalBody) {
   showNotification('Please enter a notification message to send', 'error');
   return;
  }

  setIsSyncing(true);
  try {
   let baseUrl = apiEndpoint.trim().replace(/\/eap_data\.json.*$/, '').replace(/\/+$/, '');
   if (!baseUrl.includes('firebaseio.com')) {
    baseUrl = 'https://eap-tracker-default-rtdb.firebaseio.com';
   }
   const targetUrl = `${baseUrl}/broadcast_notification.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`;

   const res = await fetch(targetUrl, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
     title: finalTitle,
     message: finalBody,
     category: finalCat,
     timestamp: Date.now(),
     actionUrl: directNotifUrl.trim() || undefined,
     actionButtonText: directNotifButtonText.trim() || (directNotifUrl.trim() ? 'Open Link' : undefined),
     actionRoute: directNotifRoute.trim() || undefined
    })
   });

   if (res.ok) {
    // Trigger instant FCM push for closed Android apps
    const pushSuccess = await dispatchFCMTopicPush(finalTitle, finalBody, 'eap_updates', {
     category: finalCat,
     actionUrl: directNotifUrl.trim() || '',
     actionButtonText: directNotifButtonText.trim() || '',
     actionRoute: directNotifRoute.trim() || ''
    });
    
    if (pushSuccess) {
     sound.playSuccess();
     showNotification(`Instant Push Notification sent to all students!`);
    } else {
     sound.playNegative();
     showNotification(`Data published successfully, but notification sending failed.`, 'error');
    }
    if (!body) {
     setDirectNotifBody('');
     setDirectNotifUrl('');
     setDirectNotifButtonText('');
     setDirectNotifRoute('');
    }
   } else {
    sound.playNegative();
    showNotification(`Failed to send notification: HTTP ${res.status}`, 'error');
   }
  } catch (err: any) {
   sound.playNegative();
   showNotification(`Notification Error: ${err.message}`, 'error');
  } finally {
   setIsSyncing(false);
  }
 };

 // Set Daily Active Motivation Quote on Student Apps
 const handleSetDailyQuote = async (quoteText: string, author: string = 'Admission Mentor') => {
  setIsSyncing(true);
  try {
   let baseUrl = apiEndpoint.trim().replace(/\/eap_data\.json.*$/, '').replace(/\/+$/, '');
   if (!baseUrl.includes('firebaseio.com')) {
    baseUrl = 'https://eap-tracker-default-rtdb.firebaseio.com';
   }
   const targetUrl = `${baseUrl}/daily_quote_config.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`;

   const res = await fetch(targetUrl, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
     quoteText: quoteText.trim(),
     author: author.trim(),
     timestamp: Date.now()
    })
   });

   if (res.ok) {
    sound.playSuccess();
    showNotification(`Set active Daily Motivation Quote for all students!`);
   } else {
    sound.playNegative();
    showNotification(`Failed to set daily quote: HTTP ${res.status}`, 'error');
   }
  } catch (err: any) {
   sound.playNegative();
   showNotification(`Quote Error: ${err.message}`, 'error');
  } finally {
   setIsSyncing(false);
  }
 };

 // Broadcast Official Target Exam & Countdown Date
 const handleBroadcastTargetExam = async () => {
  const examName = targetExamPreset === 'Custom' ? targetExamCustomName.trim() : targetExamPreset;
  if (!examName) {
   showNotification('Please provide a target exam name', 'error');
   return;
  }

  setIsSyncing(true);
  try {
   const examEpoch = Date.now() + targetExamDays * 24 * 60 * 60 * 1000;
   let baseUrl = apiEndpoint.trim().replace(/\/eap_data\.json.*$/, '').replace(/\/+$/, '');
   if (!baseUrl.includes('firebaseio.com')) {
    baseUrl = 'https://eap-tracker-default-rtdb.firebaseio.com';
   }
   const targetUrl = `${baseUrl}/target_exam_config.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`;

   const res = await fetch(targetUrl, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
     examName,
     examEpoch,
     daysAhead: targetExamDays,
     timestamp: Date.now()
    })
   });

   if (res.ok) {
    sound.playSuccess();
    showNotification(`Official countdown broadcasted: "${examName}" (${targetExamDays} days left)!`);
   } else {
    sound.playNegative();
    showNotification(`Failed to broadcast exam countdown: HTTP ${res.status}`, 'error');
   }
  } catch (err: any) {
   sound.playNegative();
   showNotification(`Countdown Error: ${err.message}`, 'error');
  } finally {
   setIsSyncing(false);
  }
 };

 // Add Custom Quote
 const handleAddCustomQuote = () => {
  if (!customQuoteText.trim()) {
   showNotification('Please enter quote text', 'error');
   return;
  }
  const newQuote: MotivationQuoteItem = {
   id: `custom-${Date.now()}`,
   quote: customQuoteText.trim(),
   category: 'motivational',
   author: customQuoteAuthor.trim() || 'Admission Mentor'
  };
  setQuotesList([newQuote, ...quotesList]);
  setCustomQuoteText('');
  sound.playSuccess();
  showNotification('Added custom motivation quote to library!');
 };

 // Copy Master JSON
 const handleCopyJson = () => {
  const jsonStr = JSON.stringify(generatePayload(), null, 2);
  navigator.clipboard.writeText(jsonStr);
  sound.playSuccess();
  showNotification('Master JSON copied to clipboard!');
 };

 // If NOT Authenticated, show sleek Lock Screen
 if (!isAuthenticated) {
  return (
    <div style={{ minHeight: '80vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 20 }}>
      <div
        className="haze-card"
        style={{
          maxWidth: 440,
          width: '100%',
          borderRadius: '28px',
          padding: '36px 28px',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          textAlign: 'center',
          gap: 20,
          boxShadow: '0 20px 60px rgba(0,0,0,0.6)'
        }}
      >
        <div
          style={{
            width: 68,
            height: 68,
            borderRadius: '22px',
            background: 'linear-gradient(135deg, rgba(208, 188, 255, 0.25), rgba(182, 157, 248, 0.15))',
            border: '1px solid rgba(208, 188, 255, 0.35)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#d0bcff',
            boxShadow: '0 8px 24px rgba(0,0,0,0.3)'
          }}
        >
          <Shield size={34} />
        </div>

        <div>
          <h2 style={{ margin: 0, fontSize: 22, fontWeight: 900, color: '#fff', letterSpacing: '1px' }}>
            EAP MASTER ADMIN
          </h2>
          <p style={{ margin: '6px 0 0', fontSize: 13, color: 'rgba(255, 255, 255, 0.65)' }}>
            Sign in with verified Google Admin account or enter Master Password.
          </p>
        </div>

        {/* 1-Click Google Sign In Button */}
        <button
          type="button"
          onClick={handleGoogleSignIn}
          disabled={authLoading}
          style={{
            width: '100%',
            padding: '13px 18px',
            borderRadius: '14px',
            background: 'rgba(255, 255, 255, 0.08)',
            border: '1px solid rgba(255, 255, 255, 0.22)',
            color: '#fff',
            fontSize: 14.5,
            fontWeight: 800,
            cursor: authLoading ? 'not-allowed' : 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 12,
            transition: 'all 0.2s ease',
            boxShadow: '0 4px 14px rgba(0,0,0,0.25)'
          }}
        >
          <svg width="18" height="18" viewBox="0 0 24 24">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"/>
          </svg>
          <span>Sign in with Google</span>
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: 10, width: '100%', margin: '2px 0' }}>
          <div style={{ flex: 1, height: 1, background: 'rgba(255, 255, 255, 0.12)' }} />
          <span style={{ fontSize: 11, color: 'rgba(255, 255, 255, 0.45)', fontWeight: 700, textTransform: 'uppercase' }}>or use master key</span>
          <div style={{ flex: 1, height: 1, background: 'rgba(255, 255, 255, 0.12)' }} />
        </div>

        <form onSubmit={handleLogin} style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: 14 }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6, textAlign: 'left' }}>
            <label style={{ fontSize: 12, fontWeight: 700, color: '#d0bcff' }}>Master Password</label>
            <input
              type="password"
              placeholder={lockoutSec > 0 ? `Locked (${lockoutSec}s)` : "Enter Admin Password"}
              value={passwordInput}
              disabled={lockoutSec > 0 || authLoading}
              onChange={(e) => setPasswordInput(e.target.value)}
              autoFocus
              style={{
                width: '100%',
                padding: '14px',
                borderRadius: '14px',
                background: 'rgba(255, 255, 255, 0.06)',
                border: authError ? '1px solid #ef5350' : '1px solid rgba(208, 188, 255, 0.25)',
                color: '#fff',
                fontSize: 16,
                textAlign: 'center',
                letterSpacing: '3px'
              }}
            />
            {authError && (
              <span style={{ fontSize: 12, color: '#ef5350', fontWeight: 600 }}>{authError}</span>
            )}
          </div>

          <button
            type="submit"
            disabled={lockoutSec > 0 || authLoading}
            style={{
              width: '100%',
              padding: '14px',
              borderRadius: '14px',
              background: lockoutSec > 0 ? 'rgba(239, 83, 80, 0.2)' : '#d0bcff',
              color: lockoutSec > 0 ? '#ef5350' : '#381e72',
              border: lockoutSec > 0 ? '1px solid rgba(239, 83, 80, 0.4)' : 'none',
              fontSize: 15,
              fontWeight: 800,
              cursor: lockoutSec > 0 || authLoading ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 8,
              marginTop: 6,
              boxShadow: lockoutSec > 0 ? 'none' : '0 6px 20px rgba(208, 188, 255, 0.35)'
            }}
          >
            {lockoutSec > 0 ? (
              <>
                <Lock size={18} />
                <span>Locked ({lockoutSec}s cooldown)</span>
              </>
            ) : authLoading ? (
              <span>Verifying Credentials...</span>
            ) : (
              <>
                <Unlock size={18} />
                <span>Unlock Master Admin</span>
              </>
            )}
          </button>

          {onOpenStudentPreview && (
            <button
              type="button"
              onClick={onOpenStudentPreview}
              style={{
                background: 'transparent',
                border: 'none',
                color: 'rgba(255, 255, 255, 0.55)',
                fontSize: 13,
                cursor: 'pointer',
                marginTop: 4,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 6
              }}
            >
              <User size={14} />
              <span>Preview Student App</span>
            </button>
          )}
        </form>
      </div>
    </div>
  );
 }

 // Authenticated Admin Portal UI
 const currentRoutines = routineMode === 'Offline' ? offlineRoutines : onlineRoutines;
 const currentExams = examMode === 'Offline' ? offlineExams : onlineExams;

 return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20, width: '100%', paddingBottom: 60 }}>
   {/* Permanent Hidden file inputs for JSON upload across all tabs & modals */}
   <input
    type="file"
    ref={routineJsonUploadRef}
    accept=".json,application/json"
        style={{ display: 'none' }}
    onChange={handleRoutineJsonFileSelect}
   />
   <input
    type="file"
    ref={examJsonUploadRef}
    accept=".json,application/json"
        style={{ display: 'none' }}
    onChange={handleImportJSON}
   />
   <input
    type="file"
    ref={fileInputRef}
    accept=".json,application/json"
        style={{ display: 'none' }}
    onChange={handleImportJSON}
   />

   {/* Toast Notification */}
   {toastMessage && (
    <div
          style={{
      position: 'fixed',
      top: 24,
      right: 24,
      zIndex: 9999,
      padding: '12px 20px',
      borderRadius: '14px',
      background: toastMessage.type === 'success' ? '#81c784' : '#ef5350',
      color: toastMessage.type === 'success' ? '#1b5e20' : '#fff',
      fontWeight: 800,
      fontSize: 14,
      boxShadow: '0 8px 32px rgba(0, 0, 0, 0.4)',
      display: 'flex',
      alignItems: 'center',
      gap: 10
     }}
    >
     {toastMessage.type === 'success' ? <Check size={18} /> : <X size={18} />}
     {toastMessage.text}
    </div>
   )}

   {/* Header Bar */}
   <div
     className="haze-card admin-header-responsive"
     style={{
       borderRadius: '24px',
       padding: '16px 24px',
       display: 'flex',
       alignItems: 'center',
       justifyContent: 'space-between',
       flexWrap: 'wrap',
       gap: 16
     }}
   >
     <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
       <div
         style={{
           width: 44,
           height: 44,
           borderRadius: '14px',
           background: 'rgba(208, 188, 255, 0.2)',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           color: '#d0bcff'
         }}
       >
         <Shield size={24} />
       </div>
       <div>
         <h1 style={{ margin: 0, fontSize: 20, fontWeight: 900, color: '#fff', letterSpacing: '1px' }}>
           EAP TRACKER MASTER ADMIN
         </h1>
         <p style={{ margin: 0, fontSize: 12, color: '#d0bcff' }}>
           Centralized Schedule, Exam & Syllabus Management
         </p>
       </div>
     </div>

     <div className="admin-header-buttons" style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
      {/* Google Admin Profile Badge */}
      {googleAdminUser && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            padding: '5px 12px 5px 6px',
            borderRadius: '14px',
            background: 'rgba(208, 188, 255, 0.15)',
            border: '1px solid rgba(208, 188, 255, 0.3)'
          }}
        >
          {googleAdminUser.photoURL ? (
            <img
              src={googleAdminUser.photoURL}
              alt="Admin"
              style={{ width: 26, height: 26, borderRadius: '50%', border: '1px solid #d0bcff' }}
            />
          ) : (
            <User size={16} color="#d0bcff" />
          )}
          <div style={{ display: 'flex', flexDirection: 'column', textAlign: 'left' }}>
            <span style={{ fontSize: 12, fontWeight: 800, color: '#fff', lineHeight: 1.2 }}>
              {googleAdminUser.displayName || 'Google Admin'}
            </span>
            <span style={{ fontSize: 10, color: '#d0bcff', lineHeight: 1 }}>
              {googleAdminUser.email || 'Verified'}
            </span>
          </div>
        </div>
      )}

      {/* PWA Install App Button */}
      {onInstallApp && (
        <button
          onClick={onInstallApp}
          title="Install EAP Admin as Standalone App on PC or Mobile"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 7,
            padding: '9px 15px',
            borderRadius: '12px',
            background: 'linear-gradient(135deg, rgba(0, 229, 255, 0.22), rgba(0, 184, 212, 0.15))',
            border: '1px solid rgba(0, 229, 255, 0.4)',
            color: '#00e5ff',
            fontWeight: 800,
            fontSize: 12.5,
            cursor: 'pointer',
            boxShadow: '0 4px 14px rgba(0, 229, 255, 0.2)'
          }}
        >
          <Smartphone size={15} />
          <span>Install App</span>
        </button>
      )}

      {/* Security & Password Settings */}
      <button
        onClick={() => {
          setCurrentPasswordInput('');
          setNewPasswordInput('');
          setConfirmPasswordInput('');
          setChangePasswordError('');
          setIsChangePasswordModalOpen(true);
        }}
        title="Change Master Admin Password & Security"
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 7,
          padding: '9px 15px',
          borderRadius: '12px',
          background: 'rgba(255, 213, 79, 0.14)',
          border: '1px solid rgba(255, 213, 79, 0.35)',
          color: '#ffd54f',
          fontWeight: 800,
          fontSize: 12.5,
          cursor: 'pointer'
        }}
      >
        <Key size={15} />
        <span>Security</span>
      </button>

      {/* Student App Preview */}
      {onOpenStudentPreview && (
        <button
          onClick={onOpenStudentPreview}
          title="Preview Student App Mode"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 7,
            padding: '9px 15px',
            borderRadius: '12px',
            background: 'rgba(255, 255, 255, 0.06)',
            border: '1px solid rgba(255, 255, 255, 0.15)',
            color: 'rgba(255, 255, 255, 0.85)',
            fontWeight: 700,
            fontSize: 12.5,
            cursor: 'pointer'
          }}
        >
          <User size={15} />
          <span>Student Preview</span>
        </button>
      )}

      {/* Quick JSON Backup & Export Hub Buttons */}
      <button
       onClick={() => setIsExportHubModalOpen(true)}
       title="Open Export & Download Hub (All Routines, Exams, or Modules)"
             style={{
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        padding: '9px 16px',
        borderRadius: '12px',
        background: 'linear-gradient(135deg, rgba(208, 188, 255, 0.25), rgba(124, 77, 255, 0.2))',
        border: '1px solid rgba(208, 188, 255, 0.4)',
        color: '#d0bcff',
        fontWeight: 800,
        fontSize: 12.5,
        cursor: 'pointer',
        boxShadow: '0 4px 14px rgba(0,0,0,0.2)'
       }}
      >
       <Download size={15} />
       <span>Export Hub (7 Options)</span>
      </button>

      <button
       onClick={() => fileInputRef.current?.click()}
       title="Upload any JSON file to update routines and exams"
             style={{
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        padding: '9px 16px',
        borderRadius: '12px',
        background: 'rgba(129, 199, 132, 0.16)',
        border: '1px solid rgba(129, 199, 132, 0.35)',
        color: '#81c784',
        fontWeight: 800,
        fontSize: 12.5,
        cursor: 'pointer'
       }}
      >
       <Upload size={15} />
       <span>Universal Import</span>
      </button>

      {/* Hidden File Input for JSON restore */}
      <input
       type="file"
       ref={fileInputRef}
       accept=".json,application/json"
             style={{ display: 'none' }}
       onChange={handleImportJSON}
      />

      {/* Cloud Sync Status Badge */}
      <div
             style={{
        display: 'flex',
        alignItems: 'center',
        gap: 6,
        padding: '6px 14px',
        borderRadius: '12px',
        background: 'rgba(208, 188, 255, 0.12)',
        border: '1px solid rgba(208, 188, 255, 0.25)',
        fontSize: 12,
        color: '#d0bcff',
        fontWeight: 700
       }}
      >
       <Sparkles size={14} />
       <span>Last Sync: {lastSyncTime}</span>
      </div>

      <button
       onClick={handleLogout}
             style={{
        padding: '8px 16px',
        borderRadius: '12px',
        background: 'rgba(239, 83, 80, 0.15)',
        border: '1px solid rgba(239, 83, 80, 0.3)',
        color: '#ef5350',
        fontWeight: 700,
        fontSize: 12,
        cursor: 'pointer'
       }}
      >
       Lock Admin
      </button>
     </div>
   </div>

   {/* 9 Connected Navigation Tabs */}
   <div
     style={{
       display: 'flex',
       gap: 8,
       minHeight: 48,
       overflowX: 'auto',
       WebkitOverflowScrolling: 'touch',
       paddingBottom: 4,
       scrollbarWidth: 'none',
       msOverflowStyle: 'none'
     }}
   >
     {[
       { id: 'routines', label: 'Class Routines', icon: Calendar },
       { id: 'students', label: `Student Roster (${studentsList.length})`, icon: Users },
       { id: 'notifications', label: 'Notifications Hub', icon: Bell },
       { id: 'exams', label: 'Exam Schedules', icon: FileText },
       { id: 'quotes', label: 'Quotes & Motivation', icon: Quote },
       { id: 'syllabus', label: 'Syllabus & Subjects', icon: BookOpen },
       { id: 'presets', label: `Presets & Tracks (${presetsList.length})`, icon: Sparkles },
       { id: 'cloud', label: 'Universal REST API Hub', icon: CloudUpload },
       { id: 'help', label: 'Admin Guide & Docs', icon: HelpCircle }
     ].map((tab) => {
       const isSel = activeTab === tab.id;
       const IconComp = tab.icon;
       return (
         <div
           key={tab.id}
           onClick={() => {
             sound.playTick();
             setActiveTabWithRoute(tab.id as AdminTabId);
           }}
           style={{
             flex: isSel ? '1.2 0 auto' : '1 0 auto',
             flexShrink: 0,
             whiteSpace: 'nowrap',
             padding: '12px 18px',
             borderRadius: isSel ? '16px' : '12px',
             background: isSel ? '#d0bcff' : 'rgba(73, 69, 79, 0.45)',
             color: isSel ? '#381e72' : 'rgba(255, 255, 255, 0.75)',
             display: 'flex',
             alignItems: 'center',
             justifyContent: 'center',
             gap: 8,
             fontWeight: 800,
             fontSize: 13,
             cursor: 'pointer',
             transition: 'all 0.2s ease',
             boxShadow: isSel ? '0 4px 16px rgba(208, 188, 255, 0.35)' : 'none'
           }}
         >
           <IconComp size={16} />
           <span>{tab.label}</span>
         </div>
       );
     })}
   </div>

   {/* TAB 1: ROUTINES MANAGER */}
   {activeTab === 'routines' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
     {/* Controls Bar */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
            <div style={{ display: 'flex', gap: 6 }}>
       {(['Offline', 'Online'] as const).map((mode) => (
        <div
         key={mode}
         onClick={() => setRoutineMode(mode)}
                  style={{
          padding: '8px 18px',
          borderRadius: '12px',
          background: routineMode === mode ? '#d0bcff' : 'rgba(255, 255, 255, 0.08)',
          color: routineMode === mode ? '#381e72' : '#fff',
          fontWeight: 800,
          fontSize: 13,
          cursor: 'pointer'
         }}
        >
         {mode} Routine ({mode === 'Offline' ? offlineRoutines.length : onlineRoutines.length} Days)
        </div>
       ))}
      </div>

            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
       {/* Push Routine Mode Live to Cloud */}
       <button
        onClick={() => handlePublishModule(routineMode === 'Offline' ? 'offlineRoutine' : 'onlineRoutine')}
        disabled={isSyncing}
        title={`Push only ${routineMode} routine live to Firebase RTDB for all students`}
                style={{
         padding: '8px 15px',
         borderRadius: '12px',
         background: 'linear-gradient(135deg, rgba(0, 229, 255, 0.3), rgba(0, 150, 255, 0.25))',
         border: '1px solid rgba(0, 229, 255, 0.5)',
         color: '#00e5ff',
         fontSize: 12,
         fontWeight: 800,
         cursor: isSyncing ? 'default' : 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6,
         boxShadow: '0 2px 10px rgba(0, 229, 255, 0.2)'
        }}
       >
        <Zap size={14} />
        <span>{isSyncing ? 'Pushing...' : `Push ${routineMode} Live`}</span>
       </button>

       {/* Export Current Routine */}
       <button
        onClick={() => routineMode === 'Offline' ? handleExportOfflineRoutineJSON() : handleExportOnlineRoutineJSON()}
        title={`Download ${routineMode} Class Routine (.json)`}
                style={{
         padding: '8px 14px',
         borderRadius: '12px',
         background: 'rgba(208, 188, 255, 0.16)',
         border: '1px solid rgba(208, 188, 255, 0.35)',
         color: '#d0bcff',
         fontSize: 12,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <Download size={14} />
        Export {routineMode}
       </button>

       {/* Export All Class Routines (Offline + Online) */}
       <button
        onClick={handleExportAllRoutinesJSON}
        title="Download Both Offline & Online Class Routines (.json)"
                style={{
         padding: '8px 14px',
         borderRadius: '12px',
         background: 'rgba(208, 188, 255, 0.1)',
         border: '1px solid rgba(208, 188, 255, 0.25)',
         color: '#d0bcff',
         fontSize: 12,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <Layers size={14} />
        Export All
       </button>

       {/* Save Routine as Reusable Preset */}
       <button
        onClick={() => {
         const activeDays = routineMode === 'Offline' ? offlineRoutines : onlineRoutines;
         showCustomPrompt(
          `Save Active ${routineMode} Routine as Preset Track`,
          (name) => {
           if (name && name.trim()) {
            handleSavePresetFromData(
             name.trim(),
             'Routine',
             { routineDays: activeDays, routineMode },
             `Custom ${routineMode} routine schedule created by mentor`
            );
           }
          },
          { placeholder: 'e.g. 45-Day High-Yield Admission Routine' }
         );
        }}
        title="Save current routine schedule as a reusable track preset"
                style={{
         padding: '8px 14px',
         borderRadius: '12px',
         background: 'rgba(255, 213, 79, 0.15)',
         border: '1px solid rgba(255, 213, 79, 0.4)',
         color: '#ffd54f',
         fontSize: 12,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <Star size={14} />
        <span>Save as Preset</span>
       </button>

       {/* Presets & Track Studio Link */}
       <button
        onClick={() => setActiveTabWithRoute('presets')}
        title="Open Dedicated Presets & Track Studio"
                style={{
         padding: '8px 14px',
         borderRadius: '12px',
         background: 'linear-gradient(135deg, rgba(208, 188, 255, 0.25), rgba(182, 157, 248, 0.2))',
         border: '1px solid rgba(208, 188, 255, 0.4)',
         color: '#d0bcff',
         fontSize: 12,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <Sparkles size={14} />
        <span>Presets Studio ({presetsList.length})</span>
       </button>

       {/* Hidden file input for routine json upload */}
       <input
        type="file"
        ref={routineJsonUploadRef}
        accept=".json,application/json"
                style={{ display: 'none' }}
        onChange={handleRoutineJsonFileSelect}
       />
       <button
        onClick={() => routineJsonUploadRef.current?.click()}
        title="Upload routine JSON to review & apply"
                style={{
         padding: '8px 14px',
         borderRadius: '12px',
         background: 'rgba(0, 229, 255, 0.15)',
         border: '1px solid rgba(0, 229, 255, 0.35)',
         color: '#00e5ff',
         fontSize: 12,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <FileUp size={14} />
        Import JSON
       </button>

       <button
        onClick={() => {
         showCustomConfirm(
          `Reset ${routineMode} Routine?`,
          `Are you sure you want to reset the ${routineMode} routine back to the original bundle template?`,
          () => {
           if (routineMode === 'Offline') setOfflineRoutines(initialRoutines);
           else setOnlineRoutines(initialRoutines.slice(0, 14));
           showNotification(`Reset ${routineMode} routine to default`);
          },
          { confirmText: 'Reset Routine', cancelText: 'Cancel', isDanger: true }
         );
        }}
                style={{
         padding: '8px 12px',
         borderRadius: '12px',
         background: 'rgba(255, 255, 255, 0.06)',
         border: '1px solid rgba(255, 255, 255, 0.15)',
         color: 'rgba(255, 255, 255, 0.7)',
         fontSize: 12,
         fontWeight: 700,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 5
        }}
       >
        <RotateCcw size={13} />
        Reset {routineMode}
       </button>

       <button
        onClick={() => setIsAddingRoutine(true)}
                style={{
         padding: '8px 18px',
         borderRadius: '12px',
         background: '#d0bcff',
         color: '#381e72',
         border: 'none',
         fontSize: 13,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <Plus size={16} />
        Add Day
       </button>

       <button
        onClick={() => {
         const parseDateEpoch = (d: string): number => {
          const months: Record<string, number> = {
           Jan: 0, Feb: 1, Mar: 2, Apr: 3, May: 4, Jun: 5,
           Jul: 6, Aug: 7, Sep: 8, Oct: 9, Nov: 10, Dec: 11
          };
          const m = d.match(/^(\d{1,2})-([A-Za-z]{3})-(\d{2,4})$/);
          if (!m) return 0;
          const yr = m[3].length === 2 ? 2000 + Number(m[3]) : Number(m[3]);
          return new Date(yr, months[m[2]] ?? 0, Number(m[1])).getTime();
         };
         if (routineMode === 'Offline') {
          setOfflineRoutines(prev => [...prev].sort((a, b) => parseDateEpoch(a.date) - parseDateEpoch(b.date)));
         } else {
          setOnlineRoutines(prev => [...prev].sort((a, b) => parseDateEpoch(a.date) - parseDateEpoch(b.date)));
         }
         showNotification('Sorted by date!');
        }}
                style={{
         padding: '8px 14px',
         borderRadius: '12px',
         background: 'rgba(129, 199, 132, 0.15)',
         border: '1px solid rgba(129, 199, 132, 0.35)',
         color: '#81c784',
         fontSize: 12,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <ArrowUpDown size={13} />
        Sort by Date
       </button>
      </div>
     </div>

     {/* Routine Days Grid with Drag-to-Reorder */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(min(100%, 320px), 1fr))', gap: 14 }}>
      {currentRoutines.map((item, idx) => (
       <div
        key={item.id || idx}
        draggable
        onDragStart={(e) => e.dataTransfer.setData('text/plain', String(idx))}
        onDragOver={(e) => e.preventDefault()}
        onDrop={(e) => {
         e.preventDefault();
         const fromIdx = Number(e.dataTransfer.getData('text/plain'));
         if (fromIdx === idx) return;
         const reorder = (arr: RoutineItem[]) => {
          const copy = [...arr];
          const [moved] = copy.splice(fromIdx, 1);
          copy.splice(idx, 0, moved);
          return copy;
         };
         if (routineMode === 'Offline') setOfflineRoutines(reorder);
         else setOnlineRoutines(reorder);
        }}
        className="haze-card"
                style={{
         borderRadius: '18px',
         padding: '16px',
         display: 'flex',
         flexDirection: 'column',
         gap: 10,
         border: '1px solid rgba(255, 255, 255, 0.1)',
         cursor: 'grab',
         transition: 'opacity 0.2s, transform 0.15s'
        }}
       >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {/* Drag Handle */}
                    <div title="Drag to reorder" style={{ color: 'rgba(255,255,255,0.3)', cursor: 'grab', display: 'flex', alignItems: 'center' }}>
           <GripVertical size={16} />
          </div>
          <div>
                      <span style={{ fontSize: 16, fontWeight: 900, color: '#fff' }}>{item.date}</span>
                      <span style={{ fontSize: 13, color: '#d0bcff', marginLeft: 8, fontWeight: 700 }}>({item.day})</span>
          </div>
         </div>

                  <div style={{ display: 'flex', gap: 4 }}>
          {/* Push Single Day Live Button */}
          <button
           onClick={() => handlePublishModule('singleDayRoutine', { type: routineMode, item })}
           disabled={isSyncing}
           title="Push this single day live to all student devices instantly"
                      style={{
            width: 32,
            height: 32,
            borderRadius: '8px',
            background: 'rgba(0, 229, 255, 0.16)',
            border: '1px solid rgba(0, 229, 255, 0.35)',
            color: '#00e5ff',
            cursor: isSyncing ? 'default' : 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
           }}
          >
           <Zap size={14} />
          </button>

          <button
           onClick={() => setEditingRoutineItem({ index: idx, item })}
                      style={{
            width: 32,
            height: 32,
            borderRadius: '8px',
            background: 'rgba(255, 255, 255, 0.08)',
            border: 'none',
            color: '#fff',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
           }}
          >
           <Edit2 size={14} />
          </button>
          <button
           onClick={() => {
            showCustomConfirm(
             `Delete Routine for ${item.date}?`,
             `Are you sure you want to delete the ${routineMode} routine for ${item.date}? This will also push the update live to student devices.`,
             () => {
              if (routineMode === 'Offline') {
               const updated = offlineRoutines.filter((_, i) => i !== idx);
               setOfflineRoutines(updated);
               // Directly push updated array (avoids React state closure lag)
               pushDeletedArrayToFirebase('offlineRoutine', updated);
              } else {
               const updated = onlineRoutines.filter((_, i) => i !== idx);
               setOnlineRoutines(updated);
               pushDeletedArrayToFirebase('onlineRoutine', updated);
              }
              showNotification('Routine deleted & synced to student devices!');
             },
             { confirmText: 'Delete Routine', cancelText: 'Cancel', isDanger: true }
            );
           }}
                      style={{
            width: 32,
            height: 32,
            borderRadius: '8px',
            background: 'rgba(239, 83, 80, 0.15)',
            border: 'none',
            color: '#ef5350',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
           }}
          >
           <Trash2 size={14} />
          </button>
         </div>
        </div>

        {item.classSubject && (
                  <div style={{ fontSize: 13.5, fontWeight: 800, color: '#fff', display: 'flex', alignItems: 'center', gap: 6 }}>
          <BookOpen size={14} color="#d0bcff" />
                    <span>Class: <span style={{ color: '#d0bcff' }}>{item.classSubject}</span></span>
         </div>
        )}

        {item.examDetails && (
                  <div style={{ fontSize: 12.5, fontWeight: 600, color: '#ccc2dc', display: 'flex', alignItems: 'center', gap: 6 }}>
          <FileText size={13} color="#ccc2dc" />
          <span>Exam: {item.examDetails}</span>
         </div>
        )}

        {item.topics && item.topics.length > 0 && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 2, background: 'rgba(255,255,255,0.03)', padding: 8, borderRadius: 8 }}>
                    <span style={{ fontSize: 11, fontWeight: 800, color: 'rgba(255,255,255,0.6)' }}>
           Syllabus Parts ({item.topics.length}):
          </span>
          {item.topics.map((t, tIdx) => (
                      <span key={tIdx} style={{ fontSize: 12, color: 'rgba(255,255,255,0.85)' }}>
            • {t}
           </span>
          ))}
         </div>
        )}
       </div>
      ))}
     </div>
    </div>
   )}

   {/* TAB: STUDENT ROSTER & INDIVIDUAL WORKSPACE */}
   {activeTab === 'students' && (
    selectedStudentForDetail ? (
     /* ========================================================================= */
     /* FULL-SCREEN STUDENT PROFILE, TELEMETRY & CUSTOM ROUTINE/EXAMS WORKSPACE  */
     /* ========================================================================= */
          <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      {/* Top Navigation & Student Profile Header Card */}
            <div className="haze-card" style={{ borderRadius: '24px', padding: '20px 24px', display: 'flex', flexDirection: 'column', gap: 16, border: '1px solid rgba(208, 188, 255, 0.2)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <button
         onClick={() => {
          sound.playTick();
          setSelectedStudentForDetail(null);
         }}
                  style={{
          padding: '8px 16px',
          borderRadius: '12px',
          background: 'rgba(255, 255, 255, 0.08)',
          border: '1px solid rgba(255, 255, 255, 0.2)',
          color: '#fff',
          fontWeight: 800,
          fontSize: 13,
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          transition: 'all 0.2s'
         }}
        >
         <ArrowLeft size={16} />
         <span>Back to Student Roster</span>
        </button>

                <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
         <button
          onClick={() => {
           setPersonalNoticeStudentId(selectedStudentForDetail.studentId);
           setPersonalNoticeStudentName(selectedStudentForDetail.name);
           setIsPersonalNoticeModalOpen(true);
          }}
                    style={{
           padding: '8px 14px',
           borderRadius: '12px',
           background: 'rgba(208, 188, 255, 0.15)',
           border: '1px solid rgba(208, 188, 255, 0.3)',
           color: '#d0bcff',
           fontWeight: 800,
           fontSize: 12.5,
           cursor: 'pointer',
           display: 'flex',
           alignItems: 'center',
           gap: 6
          }}
         >
          <Send size={14} />
          <span>Send Notice</span>
         </button>

         <button
          onClick={() => handleOpenStudentDetail(selectedStudentForDetail)}
          disabled={isLoadingCustomRoutine}
                    style={{
           padding: '8px 14px',
           borderRadius: '12px',
           background: 'rgba(255, 255, 255, 0.06)',
           border: '1px solid rgba(255, 255, 255, 0.12)',
           color: '#fff',
           fontWeight: 800,
           fontSize: 12.5,
           cursor: 'pointer',
           display: 'flex',
           alignItems: 'center',
           gap: 6
          }}
         >
          <RefreshCw size={14} className={isLoadingCustomRoutine ? 'spin-icon' : ''} />
          <span>Refresh</span>
         </button>
        </div>
       </div>

       {/* Student Profile Info Banner */}
              <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
        {selectedStudentForDetail.photoUrl ? (
         <img
          src={selectedStudentForDetail.photoUrl}
          alt={selectedStudentForDetail.name}
                    style={{ width: 64, height: 64, borderRadius: '18px', objectFit: 'cover', border: '2px solid #00e5ff' }}
         />
        ) : (
                  <div style={{
          width: 64,
          height: 64,
          borderRadius: '18px',
          background: 'rgba(0, 229, 255, 0.15)',
          border: '2px solid rgba(0, 229, 255, 0.3)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: 24,
          fontWeight: 900,
          color: '#00e5ff'
         }}>
          {selectedStudentForDetail.name ? selectedStudentForDetail.name.charAt(0).toUpperCase() : 'S'}
         </div>
        )}

                <div style={{ flex: 1, minWidth: 240 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                    <h3 style={{ margin: 0, fontSize: 20, fontWeight: 900, color: '#fff' }}>
           {selectedStudentForDetail.name}
          </h3>
                    <span style={{ fontSize: 12, fontWeight: 800, background: 'rgba(0, 229, 255, 0.15)', color: '#00e5ff', padding: '3px 10px', borderRadius: 8 }}>
           {selectedStudentForDetail.studentId}
          </span>
                    <span style={{ fontSize: 12, fontWeight: 800, background: 'rgba(255, 213, 79, 0.15)', color: '#ffd54f', padding: '3px 10px', borderRadius: 8, display: 'inline-flex', alignItems: 'center', gap: 4 }}>
           <Target size={13} />
           <span>{selectedStudentForDetail.targetInstitution || 'Engineering Admission'}</span>
          </span>
         </div>
                  <div style={{ fontSize: 13, color: 'rgba(255,255,255,0.6)', marginTop: 4 }}>
          {selectedStudentForDetail.college || 'College N/A'} • {selectedStudentForDetail.batch || 'Batch N/A'} • Preferred Track: {selectedStudentForDetail.preferredTrack || 'Offline'}
         </div>
        </div>

        {/* Device & Live State */}
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
         {selectedStudentForDetail.isStudying ? (
                    <span style={{ fontSize: 12, fontWeight: 900, color: '#00e676', background: 'rgba(0, 230, 118, 0.15)', border: '1px solid rgba(0, 230, 118, 0.4)', padding: '4px 12px', borderRadius: 10, display: 'flex', alignItems: 'center', gap: 6 }}>
                      <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#00e676', boxShadow: '0 0 8px #00e676' }} />
           <span>STUDYING: {selectedStudentForDetail.activeSubject || 'Focus'}</span>
          </span>
         ) : (
                    <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.5)', background: 'rgba(255,255,255,0.05)', padding: '4px 10px', borderRadius: 8 }}>
            Idle {selectedStudentForDetail.lastActiveEpoch ? `(Active ${new Date(selectedStudentForDetail.lastActiveEpoch).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})` : ''}
          </span>
         )}
                  <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.4)' }}>
          Device: {selectedStudentForDetail.deviceModel || 'Android'}
         </span>
        </div>
       </div>

       {/* Quick Metrics Grid */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 200px), 1fr))', gap: 12, marginTop: 4 }}>
                <div style={{ background: 'rgba(255,255,255,0.04)', padding: '12px 16px', borderRadius: '14px', border: '1px solid rgba(255,255,255,0.06)' }}>
                  <div style={{ fontSize: 11, fontWeight: 700, color: 'rgba(255,255,255,0.5)' }}>TODAY STUDY</div>
                  <div style={{ fontSize: 18, fontWeight: 900, color: '#00e5ff', marginTop: 2 }}>
                    {(selectedStudentForDetail.todayStudyHours || 0).toFixed(1)}h <span style={{ fontSize: 12, fontWeight: 600, color: 'rgba(255,255,255,0.4)' }}>/ {selectedStudentForDetail.dailyGoalHours || 8}h goal</span>
         </div>
        </div>
                <div style={{ background: 'rgba(255,255,255,0.04)', padding: '12px 16px', borderRadius: '14px', border: '1px solid rgba(255,255,255,0.06)' }}>
                  <div style={{ fontSize: 11, fontWeight: 700, color: 'rgba(255,255,255,0.5)' }}>SYLLABUS PROGRESS</div>
                  <div style={{ fontSize: 18, fontWeight: 900, color: '#81c784', marginTop: 2 }}>
                    {selectedStudentForDetail.syllabusPct || 0}% <span style={{ fontSize: 12, fontWeight: 600, color: 'rgba(255,255,255,0.4)' }}>({selectedStudentForDetail.completedChapters || 0}/{selectedStudentForDetail.totalChapters || 65} chapters)</span>
         </div>
        </div>
                <div style={{ background: 'rgba(255,255,255,0.04)', padding: '12px 16px', borderRadius: '14px', border: '1px solid rgba(255,255,255,0.06)' }}>
                  <div style={{ fontSize: 11, fontWeight: 700, color: 'rgba(255,255,255,0.5)' }}>EXAM AVERAGE</div>
                  <div style={{ fontSize: 18, fontWeight: 900, color: '#ffb74d', marginTop: 2 }}>
                    {selectedStudentForDetail.examAveragePct || 0}% <span style={{ fontSize: 12, fontWeight: 600, color: 'rgba(255,255,255,0.4)' }}>({selectedStudentForDetail.totalExamsLogged || 0} exams logged)</span>
         </div>
        </div>
       </div>
      </div>
       
      {/* Sub-Tab Navigation Bar with Auto Custom Detection */}
       {(() => {
        const offlineRoutineList = (studentCustomRoutine && studentCustomRoutine.length > 0 ? studentCustomRoutine : offlineRoutines);
        const onlineRoutineList = (studentCustomOnlineRoutine && studentCustomOnlineRoutine.length > 0 ? studentCustomOnlineRoutine : onlineRoutines);
        const offlineExamsList = (studentCustomOfflineExams && studentCustomOfflineExams.length > 0 ? studentCustomOfflineExams : offlineExams);
        const onlineExamsList = (studentCustomOnlineExams && studentCustomOnlineExams.length > 0 ? studentCustomOnlineExams : onlineExams);

        const customOfflineRoutineCount = offlineRoutineList.filter(item => isRoutineItemCustomized(item, offlineRoutines)).length;
        const customOnlineRoutineCount = onlineRoutineList.filter(item => isRoutineItemCustomized(item, onlineRoutines)).length;
        const customOfflineExamCount = offlineExamsList.filter(item => isExamItemCustomized(item, offlineExams)).length;
        const customOnlineExamCount = onlineExamsList.filter(item => isExamItemCustomized(item, onlineExams)).length;

        const studentSyllabusList: Subject[] = (studentCustomSyllabus && studentCustomSyllabus.length > 0 ? studentCustomSyllabus : subjects);
        const isSyllabusCustom = Boolean(studentCustomSyllabus && studentCustomSyllabus.length > 0);

        const tabsList = [
         { id: 'offlineRoutine' as const, label: 'Offline Routine', icon: Calendar, customCount: customOfflineRoutineCount, totalCount: offlineRoutineList.length },
         { id: 'onlineRoutine' as const, label: 'Online Routine', icon: Globe, customCount: customOnlineRoutineCount, totalCount: onlineRoutineList.length },
         { id: 'offlineExams' as const, label: 'Offline Exams', icon: FileText, customCount: customOfflineExamCount, totalCount: offlineExamsList.length },
         { id: 'onlineExams' as const, label: 'Online Exams', icon: Layers, customCount: customOnlineExamCount, totalCount: onlineExamsList.length },
         { id: 'customSyllabus' as const, label: 'Custom Syllabus', icon: BookOpen, customCount: isSyllabusCustom ? studentSyllabusList.length : 0, totalCount: studentSyllabusList.length }
        ];

        let currentTabCustomCount = 0;
        if (studentDetailSubTab === 'offlineRoutine') currentTabCustomCount = customOfflineRoutineCount;
        else if (studentDetailSubTab === 'onlineRoutine') currentTabCustomCount = customOnlineRoutineCount;
        else if (studentDetailSubTab === 'offlineExams') currentTabCustomCount = customOfflineExamCount;
        else if (studentDetailSubTab === 'onlineExams') currentTabCustomCount = customOnlineExamCount;
        else currentTabCustomCount = isSyllabusCustom ? studentSyllabusList.length : 0;

        return (
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
                    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
           {tabsList.map((tab) => {
            const isSel = studentDetailSubTab === tab.id;
            const IconComp = tab.icon;
            const hasCustom = tab.customCount > 0;
            return (
             <button
              key={tab.id}
              onClick={() => {
               sound.playTick();
               setStudentDetailSubTab(tab.id);
               setSelectedStudentRoutineIndices([]);
               setSelectedStudentExamIndices([]);
               setSelectedStudentSubjectIndices([]);
              }}
                            style={{
               padding: '10px 16px',
               borderRadius: '14px',
               background: isSel ? '#d0bcff' : 'rgba(255, 255, 255, 0.08)',
               color: isSel ? '#381e72' : '#fff',
               border: 'none',
               fontWeight: 800,
               fontSize: 13,
               cursor: 'pointer',
               display: 'flex',
               alignItems: 'center',
               gap: 8,
               transition: 'all 0.2s'
              }}
             >
              <IconComp size={14} />
              <span>{tab.label} ({tab.totalCount})</span>
                            <span style={{
               fontSize: 10,
               fontWeight: 900,
               padding: '2px 7px',
               borderRadius: 6,
               background: hasCustom ? (isSel ? '#00e5ff' : 'rgba(0, 229, 255, 0.25)') : (isSel ? 'rgba(56, 30, 114, 0.2)' : 'rgba(255, 255, 255, 0.12)'),
               color: hasCustom ? (isSel ? '#00363a' : '#00e5ff') : (isSel ? '#381e72' : 'rgba(255, 255, 255, 0.7)'),
               border: hasCustom ? '1px solid rgba(0, 229, 255, 0.4)' : 'none'
              }}>
               {hasCustom ? `Custom (${tab.customCount}/${tab.totalCount})` : `Global (${tab.totalCount})`}
              </span>
             </button>
            );
           })}
          </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span style={{
            fontSize: 12,
            fontWeight: 800,
            color: currentTabCustomCount > 0 ? '#00e5ff' : '#81c784',
            background: currentTabCustomCount > 0 ? 'rgba(0, 229, 255, 0.12)' : 'rgba(129, 199, 132, 0.12)',
            border: `1px solid ${currentTabCustomCount > 0 ? 'rgba(0, 229, 255, 0.3)' : 'rgba(129, 199, 132, 0.3)'}`,
            padding: '6px 12px',
            borderRadius: 10,
            display: 'flex',
            alignItems: 'center',
            gap: 6
           }}>
            {currentTabCustomCount > 0 ? (
             <>
              <Sparkles size={13} color="#00e5ff" />
              <span>{currentTabCustomCount} Customized Item(s) Active</span>
             </>
            ) : (
             <>
              <Globe size={13} color="#81c784" />
              <span>100% In Sync with Global Master</span>
             </>
            )}
           </span>
          </div>
         </div>
        );
       })()}

       {/* Action Bar & Multi-Select Controls for Student Detail Workspace */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
         {studentDetailSubTab === 'customSyllabus' ? (
          <>
           <button
            onClick={() => handleSaveStudentCustomSyllabus(selectedStudentForDetail.studentId, studentCustomSyllabus && studentCustomSyllabus.length > 0 ? studentCustomSyllabus : subjects)}
            disabled={isSyncing}
                        style={{ padding: '9px 16px', borderRadius: '12px', background: 'linear-gradient(135deg, rgba(0, 229, 255, 0.35), rgba(0, 150, 255, 0.25))', border: '1px solid rgba(0, 229, 255, 0.5)', color: '#00e5ff', fontSize: 12.5, fontWeight: 800, cursor: isSyncing ? 'default' : 'pointer', display: 'flex', alignItems: 'center', gap: 6, boxShadow: '0 2px 10px rgba(0, 229, 255, 0.2)' }}
           >
            <Zap size={15} />
            <span>{isSyncing ? 'Deploying...' : 'Deploy Syllabus Live '}</span>
           </button>

           <button
            onClick={() => {
             const activeSyllabus = studentCustomSyllabus && studentCustomSyllabus.length > 0 ? studentCustomSyllabus : subjects;
             showCustomPrompt(
              'Save Student Syllabus as Reusable Preset',
              (name) => {
               if (name && name.trim()) {
                handleSavePresetFromData(
                 name.trim(),
                 'Syllabus',
                 { subjects: activeSyllabus },
                 `Custom syllabus track configured for ${selectedStudentForDetail.name}`
                );
               }
              },
              { initialValue: `${selectedStudentForDetail.name}'s Custom Syllabus Track`, placeholder: 'Enter Preset Track Name' }
             );
            }}
            title="Save this student's syllabus as a reusable preset track"
                        style={{ padding: '9px 14px', borderRadius: '12px', background: 'rgba(255, 213, 79, 0.15)', border: '1px solid rgba(255, 213, 79, 0.4)', color: '#ffd54f', fontWeight: 800, fontSize: 12.5, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}
           >
            <Star size={14} />
            <span>Save as Preset Track</span>
           </button>

           {studentCustomSyllabus && studentCustomSyllabus.length > 0 && (
            <button
             onClick={() => handleResetStudentCustomSyllabus(selectedStudentForDetail.studentId)}
             disabled={isSyncing}
                          style={{ padding: '9px 14px', borderRadius: '12px', background: 'rgba(239, 83, 80, 0.15)', border: '1px solid rgba(239, 83, 80, 0.35)', color: '#ef5350', fontWeight: 800, fontSize: 12.5, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}
            >
             <RotateCcw size={14} />
             <span>Reset to Global Syllabus</span>
            </button>
           )}
          </>
         ) : (
          <>
           <button
            onClick={handlePushStudentScheduleLive}
            disabled={isSyncing}
                        style={{ padding: '9px 16px', borderRadius: '12px', background: 'linear-gradient(135deg, rgba(0, 229, 255, 0.35), rgba(0, 150, 255, 0.25))', border: '1px solid rgba(0, 229, 255, 0.5)', color: '#00e5ff', fontSize: 12.5, fontWeight: 800, cursor: isSyncing ? 'default' : 'pointer', display: 'flex', alignItems: 'center', gap: 6, boxShadow: '0 2px 10px rgba(0, 229, 255, 0.2)' }}
           >
            <Zap size={15} />
            <span>{isSyncing ? 'Pushing...' : 'Push Custom Live'}</span>
           </button>

           {/* Save Student Routine/Exams as Reusable Preset */}
           <button
            onClick={() => {
             if (studentDetailSubTab === 'offlineRoutine' || studentDetailSubTab === 'onlineRoutine') {
              const isOnline = studentDetailSubTab === 'onlineRoutine';
              const activeDays = isOnline
               ? (studentCustomOnlineRoutine && studentCustomOnlineRoutine.length > 0 ? studentCustomOnlineRoutine : onlineRoutines)
               : (studentCustomRoutine && studentCustomRoutine.length > 0 ? studentCustomRoutine : offlineRoutines);
              showCustomPrompt(
               'Save Student Routine as Reusable Preset',
               (name) => {
                if (name && name.trim()) {
                 handleSavePresetFromData(
                  name.trim(),
                  'Routine',
                  { routineDays: activeDays, routineMode: isOnline ? 'Online' : 'Offline' },
                  `Custom ${isOnline ? 'Online' : 'Offline'} routine configured for ${selectedStudentForDetail.name}`
                 );
                }
               },
               { initialValue: `${selectedStudentForDetail.name}'s Varsity Routine`, placeholder: 'Enter Routine Preset Name' }
              );
             } else {
              const isOnline = studentDetailSubTab === 'onlineExams';
              const activeExams = isOnline
               ? (studentCustomOnlineExams && studentCustomOnlineExams.length > 0 ? studentCustomOnlineExams : onlineExams)
               : (studentCustomOfflineExams && studentCustomOfflineExams.length > 0 ? studentCustomOfflineExams : offlineExams);
              showCustomPrompt(
               'Save Student Exams as Reusable Preset',
               (name) => {
                if (name && name.trim()) {
                 handleSavePresetFromData(
                  name.trim(),
                  'Exams',
                  { exams: activeExams },
                  `Custom exam schedule configured for ${selectedStudentForDetail.name}`
                 );
                }
               },
               { initialValue: `${selectedStudentForDetail.name}'s Exam Series`, placeholder: 'Enter Exam Preset Name' }
              );
             }
            }}
            title="Save this student's schedule as a reusable track preset"
                        style={{ padding: '9px 14px', borderRadius: '12px', background: 'rgba(255, 213, 79, 0.15)', border: '1px solid rgba(255, 213, 79, 0.4)', color: '#ffd54f', fontWeight: 800, fontSize: 12.5, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}
           >
            <Star size={14} />
            <span>Save as Preset Track</span>
           </button>

           <button
            onClick={handleExportStudentJSON}
                        style={{ padding: '9px 14px', borderRadius: '12px', background: 'rgba(208, 188, 255, 0.16)', border: '1px solid rgba(208, 188, 255, 0.35)', color: '#d0bcff', fontSize: 12.5, fontWeight: 800, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}
           >
            <Download size={14} />
            <span>Export</span>
           </button>
           <button
            onClick={() => { setUploadedRoutineTargetStudentId(selectedStudentForDetail.studentId); setUploadedRoutineTargetMode('Individual'); (studentDetailSubTab === 'offlineRoutine' || studentDetailSubTab === 'onlineRoutine') ? routineJsonUploadRef.current?.click() : examJsonUploadRef.current?.click(); }}
                        style={{ padding: '9px 14px', borderRadius: '12px', background: 'rgba(0, 229, 255, 0.15)', border: '1px solid rgba(0, 229, 255, 0.35)', color: '#00e5ff', fontSize: 12.5, fontWeight: 800, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}
           >
            <FileUp size={14} />
            <span>Import JSON</span>
           </button>
           {(() => {
            let hasCustom = false;
            let resetLabel = 'Reset to Global';
            let resetAction = () => {};
            if (studentDetailSubTab === 'offlineRoutine') { hasCustom = Boolean(studentCustomRoutine && studentCustomRoutine.length > 0); resetLabel = 'Reset Offline'; resetAction = () => handleResetStudentCustomRoutine(selectedStudentForDetail.studentId); }
            else if (studentDetailSubTab === 'onlineRoutine') { hasCustom = Boolean(studentCustomOnlineRoutine && studentCustomOnlineRoutine.length > 0); resetLabel = 'Reset Online'; resetAction = () => handleResetStudentCustomRoutine(selectedStudentForDetail.studentId); }
            else if (studentDetailSubTab === 'offlineExams') { hasCustom = Boolean(studentCustomOfflineExams && studentCustomOfflineExams.length > 0); resetLabel = 'Reset Offline Exams'; resetAction = () => handleResetStudentCustomExams(selectedStudentForDetail.studentId); }
            else if (studentDetailSubTab === 'onlineExams') { hasCustom = Boolean(studentCustomOnlineExams && studentCustomOnlineExams.length > 0); resetLabel = 'Reset Online Exams'; resetAction = () => handleResetStudentCustomExams(selectedStudentForDetail.studentId); }
            return hasCustom ? (
                          <button onClick={resetAction} disabled={isSyncing} style={{ padding: '9px 14px', borderRadius: '12px', background: 'rgba(239, 83, 80, 0.15)', border: '1px solid rgba(239, 83, 80, 0.35)', color: '#ef5350', fontWeight: 800, fontSize: 12.5, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}>
              <RotateCcw size={14} />
              <span>{resetLabel}</span>
             </button>
            ) : null;
           })()}
          </>
         )}
        </div>

                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
         {studentDetailSubTab === 'customSyllabus' ? (
          <button
           onClick={() => {
            showCustomPrompt(
             'Add Subject for Student',
             (newSubName) => {
              if (newSubName && newSubName.trim()) {
               const newSub: Subject = {
                id: `custom_${Date.now()}`,
                name: newSubName.trim(),
                iconName: 'BookOpen',
                papers: [
                 { id: `pap_1_${Date.now()}`, name: '1st Paper', chapters: [] },
                 { id: `pap_2_${Date.now()}`, name: '2nd Paper', chapters: [] }
                ]
               };
               setStudentCustomSyllabus((prev) => {
                const list = prev && prev.length > 0 ? [...prev] : [...subjects];
                return [...list, newSub];
               });
               showNotification(`Added custom subject "${newSubName.trim()}" for this student`);
              }
             },
             { placeholder: 'e.g. ICT, Bangla, English' }
            );
           }}
                      style={{ padding: '9px 14px', borderRadius: '12px', background: '#d0bcff', border: 'none', color: '#381e72', fontWeight: 900, fontSize: 12.5, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}
          >
           <Plus size={15} />
           <span>+ Add Subject for Student</span>
          </button>
         ) : (
          <>
           <button
            onClick={handleAddStudentDay}
                        style={{ padding: '9px 14px', borderRadius: '12px', background: '#d0bcff', border: 'none', color: '#381e72', fontWeight: 900, fontSize: 12.5, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}
           >
            <Plus size={15} />
            <span>{studentDetailSubTab.includes('Routine') ? '+ Add Day' : '+ Add Exam'}</span>
           </button>
           <button
            onClick={handleSortStudentSchedule}
                        style={{ padding: '9px 14px', borderRadius: '12px', background: 'rgba(255, 255, 255, 0.08)', border: '1px solid rgba(255, 255, 255, 0.15)', color: '#fff', fontWeight: 700, fontSize: 12.5, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}
           >
            <ArrowUpDown size={14} />
            <span>Sort</span>
           </button>
          </>
         )}
        </div>
       </div>

      {(() => {
       if (studentDetailSubTab === 'customSyllabus') {
        const studentSubjects = studentCustomSyllabus && studentCustomSyllabus.length > 0 ? studentCustomSyllabus : subjects;
        return (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                    <div style={{ padding: '12px 16px', borderRadius: 14, background: 'rgba(0, 229, 255, 0.08)', border: '1px solid rgba(0, 229, 255, 0.25)', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 10 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Sparkles size={16} color="#00e5ff" />
                        <span style={{ fontSize: 13, fontWeight: 800, color: '#00e5ff' }}>
             Personalized Curriculum Editor for {selectedStudentForDetail.name}
            </span>
           </div>
                      <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.7)' }}>
            Add, remove, reorder, or edit subjects and chapters specifically for this student.
           </span>
          </div>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
           {studentSubjects.map((sub, sIdx) => (
            <div
             key={sub.id || sIdx}
             className="haze-card"
                          style={{
              borderRadius: '20px',
              padding: '20px',
              display: 'flex',
              flexDirection: 'column',
              gap: 14,
              border: '1px solid rgba(208, 188, 255, 0.2)'
             }}
            >
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                              <div style={{ display: 'flex', gap: 2 }}>
                <button
                 onClick={() => moveSubjectUp(sIdx, true)}
                 disabled={sIdx === 0}
                 title="Move Up"
                                  style={{
                  background: sIdx === 0 ? 'rgba(255,255,255,0.02)' : 'rgba(255,255,255,0.08)',
                  border: 'none',
                  color: sIdx === 0 ? 'rgba(255,255,255,0.2)' : '#fff',
                  borderRadius: 6,
                  padding: '4px 6px',
                  cursor: sIdx === 0 ? 'default' : 'pointer'
                 }}
                >
                 <ArrowUp size={13} />
                </button>
                <button
                 onClick={() => moveSubjectDown(sIdx, true)}
                 disabled={sIdx === studentSubjects.length - 1}
                 title="Move Down"
                                  style={{
                  background: sIdx === studentSubjects.length - 1 ? 'rgba(255,255,255,0.02)' : 'rgba(255,255,255,0.08)',
                  border: 'none',
                  color: sIdx === studentSubjects.length - 1 ? 'rgba(255,255,255,0.2)' : '#fff',
                  borderRadius: 6,
                  padding: '4px 6px',
                  cursor: sIdx === studentSubjects.length - 1 ? 'default' : 'pointer'
                 }}
                >
                 <ArrowDown size={13} />
                </button>
               </div>

                              <span style={{ fontSize: 13, fontWeight: 800, color: 'rgba(255,255,255,0.4)' }}>#{sIdx + 1}</span>
                              <span style={{ fontSize: 18, fontWeight: 900, color: '#fff' }}>{sub.name}</span>
                              <span style={{ fontSize: 11, background: 'rgba(0, 229, 255, 0.15)', color: '#00e5ff', padding: '2px 8px', borderRadius: 6, fontWeight: 700 }}>
                {sub.papers.reduce((acc, p) => acc + p.chapters.length, 0)} Chapters
               </span>
              </div>

                            <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
               <button
                onClick={() => {
                 showCustomPrompt(
                  `Rename Subject "${sub.name}"`,
                  (newName) => {
                   if (newName && newName.trim()) {
                    setStudentCustomSyllabus((prev) => {
                     const list = prev && prev.length > 0 ? [...prev] : [...subjects];
                     return list.map((s, idx) => idx === sIdx ? { ...s, name: newName.trim() } : s);
                    });
                    showNotification('Subject renamed for this student');
                   }
                  },
                  { initialValue: sub.name, placeholder: 'Enter new subject name' }
                 );
                }}
                                style={{
                 background: 'rgba(208, 188, 255, 0.12)',
                 border: 'none',
                 color: '#d0bcff',
                 padding: '6px 12px',
                 borderRadius: '8px',
                 cursor: 'pointer',
                 fontSize: 12,
                 fontWeight: 700,
                 display: 'flex',
                 alignItems: 'center',
                 gap: 5
                }}
               >
                <Edit2 size={13} />
                <span>Rename</span>
               </button>

               <button
                onClick={() => {
                 showCustomConfirm(
                  `Remove Subject "${sub.name}"?`,
                  `This will remove ${sub.name} specifically for ${selectedStudentForDetail.name} (Global Master remains untouched).`,
                  () => {
                   setStudentCustomSyllabus((prev) => {
                    const list = prev && prev.length > 0 ? [...prev] : [...subjects];
                    return list.filter((_, idx) => idx !== sIdx);
                   });
                   showNotification(`Removed ${sub.name} from student's syllabus`);
                  },
                  { confirmText: 'Remove Subject', cancelText: 'Cancel', isDanger: true }
                 );
                }}
                                style={{
                 background: 'rgba(239, 83, 80, 0.15)',
                 border: 'none',
                 color: '#ef5350',
                 padding: '6px 12px',
                 borderRadius: '8px',
                 cursor: 'pointer',
                 fontSize: 12,
                 fontWeight: 700,
                 display: 'flex',
                 alignItems: 'center',
                 gap: 5
                }}
               >
                <Trash2 size={13} />
                <span>Remove Subject</span>
               </button>
              </div>
             </div>

             {/* Papers */}
                          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 280px), 1fr))', gap: 12 }}>
              {sub.papers.map((paper, pIdx) => (
               <div
                key={paper.id || pIdx}
                                style={{
                 background: 'rgba(255, 255, 255, 0.04)',
                 border: '1px solid rgba(255, 255, 255, 0.08)',
                 borderRadius: '14px',
                 padding: '14px'
                }}
               >
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                                  <span style={{ fontSize: 13.5, fontWeight: 800, color: '#d0bcff' }}>
                  {paper.name} ({paper.chapters.length} Chaps)
                 </span>
                 <button
                  onClick={() => {
                   showCustomPrompt(
                    `Add Chapter to ${sub.name} - ${paper.name}`,
                    (chapName) => {
                     if (chapName && chapName.trim()) {
                      setStudentCustomSyllabus((prev) => {
                       const list = prev && prev.length > 0 ? [...prev] : [...subjects];
                       const papers = [...list[sIdx].papers];
                       const chapters = [...papers[pIdx].chapters, { id: `ch_${Date.now()}`, name: chapName.trim(), sections: [] }];
                       papers[pIdx] = { ...papers[pIdx], chapters };
                       list[sIdx] = { ...list[sIdx], papers };
                       return list;
                      });
                      showNotification(`Added chapter "${chapName.trim()}"`);
                     }
                    },
                    { placeholder: 'Enter chapter name' }
                   );
                  }}
                                    style={{
                   background: 'rgba(208, 188, 255, 0.18)',
                   border: 'none',
                   color: '#d0bcff',
                   padding: '4px 10px',
                   borderRadius: '8px',
                   cursor: 'pointer',
                   fontSize: 11,
                   fontWeight: 800
                  }}
                 >
                  + Add Chapter
                 </button>
                </div>

                                <div style={{ display: 'flex', flexDirection: 'column', gap: 4, maxHeight: 200, overflowY: 'auto' }}>
                 {paper.chapters.map((chap, cIdx) => (
                  <div
                   key={chap.id || cIdx}
                                      style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '5px 8px',
                    background: 'rgba(255,255,255,0.02)',
                    borderRadius: 8,
                    borderBottom: '1px solid rgba(255,255,255,0.04)',
                    fontSize: 12,
                    color: 'rgba(255, 255, 255, 0.9)'
                   }}
                  >
                                      <div style={{ display: 'flex', alignItems: 'center', gap: 6, flex: 1, minWidth: 0 }}>
                                        <div style={{ display: 'flex', gap: 2 }}>
                     <button
                      onClick={() => moveChapterUp(sIdx, pIdx, cIdx, true)}
                      disabled={cIdx === 0}
                                            style={{
                       background: 'transparent',
                       border: 'none',
                       color: cIdx === 0 ? 'rgba(255,255,255,0.15)' : 'rgba(255,255,255,0.6)',
                       cursor: cIdx === 0 ? 'default' : 'pointer',
                       padding: 2
                      }}
                     >
                      <ArrowUp size={11} />
                     </button>
                     <button
                      onClick={() => moveChapterDown(sIdx, pIdx, cIdx, true)}
                      disabled={cIdx === paper.chapters.length - 1}
                                            style={{
                       background: 'transparent',
                       border: 'none',
                       color: cIdx === paper.chapters.length - 1 ? 'rgba(255,255,255,0.15)' : 'rgba(255,255,255,0.6)',
                       cursor: cIdx === paper.chapters.length - 1 ? 'default' : 'pointer',
                       padding: 2
                      }}
                     >
                      <ArrowDown size={11} />
                     </button>
                    </div>
                                        <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.4)', minWidth: 16 }}>{cIdx + 1}.</span>
                                        <span style={{ textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>{chap.name}</span>
                   </div>

                                      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    <button
                     onClick={() => {
                      showCustomPrompt(
                       `Rename Chapter`,
                       (val) => {
                        if (val && val.trim()) {
                         setStudentCustomSyllabus((prev) => {
                          const list = prev && prev.length > 0 ? [...prev] : [...subjects];
                          const papers = [...list[sIdx].papers];
                          const chapters = papers[pIdx].chapters.map((c, i) => i === cIdx ? { ...c, name: val.trim() } : c);
                          papers[pIdx] = { ...papers[pIdx], chapters };
                          list[sIdx] = { ...list[sIdx], papers };
                          return list;
                         });
                         showNotification('Chapter renamed');
                        }
                       },
                       { initialValue: chap.name, placeholder: 'Enter new chapter name' }
                      );
                     }}
                     title="Rename Chapter"
                                          style={{
                      background: 'transparent',
                      border: 'none',
                      color: '#d0bcff',
                      cursor: 'pointer',
                      padding: 2,
                      display: 'flex',
                      alignItems: 'center'
                     }}
                    >
                     <Edit2 size={11} />
                    </button>

                    <button
                     onClick={() => {
                      showCustomConfirm(
                       `Delete Chapter "${chap.name}"?`,
                       `Are you sure you want to delete chapter "${chap.name}" for this student?`,
                       () => {
                        setStudentCustomSyllabus((prev) => {
                         const list = prev && prev.length > 0 ? [...prev] : [...subjects];
                         const papers = [...list[sIdx].papers];
                         const chapters = papers[pIdx].chapters.filter((_, i) => i !== cIdx);
                         papers[pIdx] = { ...papers[pIdx], chapters };
                         list[sIdx] = { ...list[sIdx], papers };
                         return list;
                        });
                        showNotification(`Deleted chapter "${chap.name}"`);
                       },
                       { confirmText: 'Delete Chapter', cancelText: 'Cancel', isDanger: true }
                      );
                     }}
                                          style={{
                      background: 'transparent',
                      border: 'none',
                      color: '#ef5350',
                      cursor: 'pointer',
                      padding: 2,
                      display: 'flex',
                      alignItems: 'center'
                     }}
                    >
                     <X size={13} />
                    </button>
                   </div>
                  </div>
                 ))}
                </div>
               </div>
              ))}
             </div>
            </div>
           ))}
          </div>
         </div>
        );
       } else if (studentDetailSubTab === 'offlineRoutine' || studentDetailSubTab === 'onlineRoutine') {
        const isOnline = studentDetailSubTab === 'onlineRoutine';
        const globalMasterList = isOnline ? onlineRoutines : offlineRoutines;
        const currentList: RoutineItem[] = isOnline
         ? (studentCustomOnlineRoutine && studentCustomOnlineRoutine.length > 0 ? studentCustomOnlineRoutine : onlineRoutines)
         : (studentCustomRoutine && studentCustomRoutine.length > 0 ? studentCustomRoutine : offlineRoutines);

        if (currentList.length === 0) {
         return (
                    <div className="haze-card" style={{ padding: '40px 20px', textAlign: 'center', borderRadius: '20px' }}>
                      <Calendar size={40} color="rgba(208, 188, 255, 0.4)" style={{ margin: '0 auto 12px' }} />
                      <h3 style={{ margin: 0, fontSize: 16, color: '#fff', fontWeight: 800 }}>No Routine Days Configured</h3>
          </div>
         );
        }

        return (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(min(100%, 320px), 1fr))', gap: 14 }}>
          {currentList.map((item: any, idx) => {
           const isCustom = isRoutineItemCustomized(item, globalMasterList);
           return (
            <div
             key={item.id || idx}
             className="haze-card"
                          style={{
              borderRadius: '18px',
              padding: '16px',
              display: 'flex',
              flexDirection: 'column',
              gap: 10,
              border: isCustom ? '1px solid rgba(0, 229, 255, 0.35)' : '1px solid rgba(255, 255, 255, 0.1)',
              background: isCustom ? 'rgba(0, 229, 255, 0.03)' : undefined
             }}
            >
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                              <span style={{ fontSize: 11, fontWeight: 900, background: '#d0bcff', color: '#381e72', padding: '3px 8px', borderRadius: 6 }}>
                Day {item.dayNumber || (idx + 1)}
               </span>
               <div>
                                <span style={{ fontSize: 16, fontWeight: 900, color: '#fff' }}>{item.date}</span>
                                <span style={{ fontSize: 13, color: '#d0bcff', marginLeft: 8, fontWeight: 700 }}>({item.day})</span>
               </div>
                              <span style={{ fontSize: 10, fontWeight: 900, padding: '2px 7px', borderRadius: 6, background: isCustom ? 'rgba(0, 229, 255, 0.2)' : 'rgba(255, 255, 255, 0.08)', color: isCustom ? '#00e5ff' : 'rgba(255, 255, 255, 0.6)', border: isCustom ? '1px solid rgba(0, 229, 255, 0.4)' : '1px solid rgba(255, 255, 255, 0.1)', display: 'inline-flex', alignItems: 'center', gap: 3 }}>
                {isCustom ? <Sparkles size={11} /> : <Globe size={11} />}
                {isCustom ? 'Custom' : 'Global'}
               </span>
              </div>

                            <div style={{ display: 'flex', gap: 6 }}>
               <button
                onClick={() => handlePushSingleDayToStudent(selectedStudentForDetail.studentId, item, isOnline ? 'Online' : 'Offline')}
                disabled={isSyncing}
                title="Push this single day live to student device"
                                style={{ width: 32, height: 32, borderRadius: '8px', background: 'rgba(255, 213, 79, 0.15)', border: '1px solid rgba(255, 213, 79, 0.35)', color: '#ffd54f', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
               >
                <Zap size={14} />
               </button>
               {isCustom && (
                <button
                 onClick={() => handleResetSingleDayToGlobal(idx)}
                 disabled={isSyncing}
                 title="Revert this day to Global Master routine"
                                  style={{ width: 32, height: 32, borderRadius: '8px', background: 'rgba(0, 229, 255, 0.15)', border: '1px solid rgba(0, 229, 255, 0.35)', color: '#00e5ff', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                >
                 <RotateCcw size={14} />
                </button>
               )}
               <button
                onClick={() => {
                 setEditingDayIndex(idx);
                 setEditingDayData({
                  dayNumber: item.dayNumber || (idx + 1),
                  date: item.date || '',
                  dayName: item.day || '',
                  classSubject: item.classSubject || '',
                  examDetails: item.examDetails || '',
                  isExamDay: Boolean(item.isExamDay),
                  examType: item.examType || 'Daily MCQ',
                  topics: Array.isArray(item.topics) ? item.topics.join('\n') : (item.topics || '')
                 });
                }}
                title="Edit Day"
                                style={{ width: 32, height: 32, borderRadius: '8px', background: 'rgba(255, 255, 255, 0.08)', border: 'none', color: '#fff', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
               >
                <Edit2 size={14} />
               </button>
               <button
                onClick={() => handleDeleteStudentDay(idx)}
                title="Delete Day"
                                style={{ width: 32, height: 32, borderRadius: '8px', background: 'rgba(239, 83, 80, 0.15)', border: '1px solid rgba(239, 83, 80, 0.3)', color: '#ef5350', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
               >
                <Trash2 size={14} />
               </button>
              </div>
             </div>

             {item.classSubject && (
                            <div style={{ padding: '10px 12px', borderRadius: '12px', background: 'rgba(0, 229, 255, 0.08)', border: '1px solid rgba(0, 229, 255, 0.2)', display: 'flex', alignItems: 'center', gap: 6 }}>
               <BookOpen size={13} color="#00e5ff" />
                              <span style={{ fontSize: 13, fontWeight: 900, color: '#00e5ff' }}>Class: {item.classSubject}</span>
              </div>
             )}

             {item.examDetails && (
                            <div style={{ padding: '10px 12px', borderRadius: '12px', background: 'rgba(255, 183, 77, 0.08)', border: '1px solid rgba(255, 183, 77, 0.2)', display: 'flex', alignItems: 'center', gap: 6 }}>
               <Target size={13} color="#ffb74d" />
                              <span style={{ fontSize: 12.5, fontWeight: 800, color: '#ffb74d' }}>Exam: {item.examDetails}</span>
              </div>
             )}

             {item.topics && Array.isArray(item.topics) && item.topics.length > 0 && (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: 4, marginTop: 2 }}>
                              <span style={{ fontSize: 11, fontWeight: 700, color: 'rgba(255,255,255,0.5)' }}>Syllabus Parts ({item.topics.length}):</span>
               {item.topics.map((t: string, tIdx: number) => (
                                <span key={tIdx} style={{ fontSize: 12, color: 'rgba(255,255,255,0.85)' }}>• {t}</span>
               ))}
              </div>
             )}
            </div>
           );
          })}
         </div>
        );
       } else {
        const isOnline = studentDetailSubTab === 'onlineExams';
        const globalMasterExams = isOnline ? onlineExams : offlineExams;
        const currentExams: any[] = isOnline
         ? (studentCustomOnlineExams && studentCustomOnlineExams.length > 0 ? studentCustomOnlineExams : onlineExams)
         : (studentCustomOfflineExams && studentCustomOfflineExams.length > 0 ? studentCustomOfflineExams : offlineExams);

        if (currentExams.length === 0) {
         return (
                    <div className="haze-card" style={{ padding: '40px 20px', textAlign: 'center', borderRadius: '20px' }}>
                      <Target size={40} color="rgba(255, 183, 77, 0.4)" style={{ margin: '0 auto 12px' }} />
                      <h3 style={{ margin: 0, fontSize: 16, color: '#fff', fontWeight: 800 }}>No Exam Schedules</h3>
          </div>
         );
        }

        return (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(min(100%, 320px), 1fr))', gap: 14 }}>
          {currentExams.map((item, idx) => {
           const isCustom = isExamItemCustomized(item, globalMasterExams);
           return (
            <div
             key={idx}
             className="haze-card"
                          style={{
              borderRadius: '18px',
              padding: '16px',
              display: 'flex',
              flexDirection: 'column',
              gap: 10,
              border: isCustom ? '1px solid rgba(255, 183, 77, 0.35)' : '1px solid rgba(255, 255, 255, 0.1)',
              background: isCustom ? 'rgba(255, 183, 77, 0.03)' : undefined
             }}
            >
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                              <span style={{ fontSize: 11, fontWeight: 900, background: '#ffb74d', color: '#3e2723', padding: '3px 8px', borderRadius: 6 }}>{item.date || 'Exam Date'}</span>
                              <span style={{ fontSize: 13, color: '#ffb74d', fontWeight: 700 }}>{item.day || ''}</span>
                              <span style={{ fontSize: 10, fontWeight: 900, padding: '2px 7px', borderRadius: 6, background: isCustom ? 'rgba(255, 183, 77, 0.2)' : 'rgba(255, 255, 255, 0.08)', color: isCustom ? '#ffb74d' : 'rgba(255, 255, 255, 0.6)', border: isCustom ? '1px solid rgba(255, 183, 77, 0.4)' : '1px solid rgba(255, 255, 255, 0.1)', display: 'inline-flex', alignItems: 'center', gap: 3 }}>
                {isCustom ? <Sparkles size={11} /> : <Globe size={11} />}
                {isCustom ? 'Custom' : 'Global'}
               </span>
              </div>

                            <div style={{ display: 'flex', gap: 6 }}>
               <button
                onClick={() => handlePushSingleExamToStudent(selectedStudentForDetail.studentId, item, isOnline ? 'Online' : 'Offline')}
                disabled={isSyncing}
                title="Push this single exam live to student device"
                                style={{ width: 32, height: 32, borderRadius: '8px', background: 'rgba(255, 213, 79, 0.15)', border: '1px solid rgba(255, 213, 79, 0.35)', color: '#ffd54f', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
               >
                <Zap size={14} />
               </button>
               {isCustom && (
                <button
                 onClick={() => handleResetSingleExamToGlobal(idx)}
                 disabled={isSyncing}
                 title="Revert this exam to Global Master schedule"
                                  style={{ width: 32, height: 32, borderRadius: '8px', background: 'rgba(0, 229, 255, 0.15)', border: '1px solid rgba(0, 229, 255, 0.35)', color: '#00e5ff', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                >
                 <RotateCcw size={14} />
                </button>
               )}
               <button
                onClick={() => handleDeleteStudentExam(idx)}
                title="Delete Exam"
                                style={{ width: 32, height: 32, borderRadius: '8px', background: 'rgba(239, 83, 80, 0.15)', border: '1px solid rgba(239, 83, 80, 0.3)', color: '#ef5350', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
               >
                <Trash2 size={14} />
               </button>
              </div>
             </div>

                          <div style={{ fontSize: 15, fontWeight: 900, color: '#fff' }}>{item.exams || item.examName || 'Assessment'}</div>
             {item.syllabus && (
                            <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.75)', background: 'rgba(255,255,255,0.04)', padding: '8px 10px', borderRadius: 8 }}>{item.syllabus}</div>
             )}
            </div>
           );
          })}
         </div>
        );
       }
      })()}
     </div>
    ) : (
     /* ========================================================================= */
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* Header Controls */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <div style={{
         position: 'relative',
         display: 'flex',
         alignItems: 'center'
        }}>
                  <Search size={16} color="rgba(255,255,255,0.5)" style={{ position: 'absolute', left: 12 }} />
         <input
          type="text"
          placeholder="Search by student name, roll, college or target..."
          value={studentSearchQuery}
          onChange={(e) => setStudentSearchQuery(e.target.value)}
                    style={{
           padding: '10px 14px 10px 36px',
           borderRadius: '12px',
           background: 'rgba(255, 255, 255, 0.06)',
           border: '1px solid rgba(208, 188, 255, 0.25)',
           color: '#fff',
           fontSize: 13,
           minWidth: 280
          }}
         />
        </div>
                <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.6)', fontWeight: 600 }}>
         Showing {studentsList.filter(s =>
          !studentSearchQuery.trim() ||
          s.name.toLowerCase().includes(studentSearchQuery.toLowerCase()) ||
          s.studentId.toLowerCase().includes(studentSearchQuery.toLowerCase()) ||
          s.college.toLowerCase().includes(studentSearchQuery.toLowerCase()) ||
          s.targetInstitution.toLowerCase().includes(studentSearchQuery.toLowerCase())
         ).length} of {studentsList.length} registered students
        </span>
       </div>

              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
        <button
         onClick={() => setActiveTabWithRoute('presets')}
                  style={{
          padding: '8px 16px',
          borderRadius: '12px',
          background: 'linear-gradient(135deg, rgba(208, 188, 255, 0.25), rgba(182, 157, 248, 0.2))',
          border: '1px solid rgba(208, 188, 255, 0.4)',
          color: '#d0bcff',
          fontSize: 12.5,
          fontWeight: 900,
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: 6
         }}
        >
         <Sparkles size={14} />
         <span>Presets Studio ({presetsList.length})</span>
        </button>

        <button
         onClick={() => {
          const filtered = studentsList
           .filter(s =>
            !studentSearchQuery.trim() ||
            s.name.toLowerCase().includes(studentSearchQuery.toLowerCase()) ||
            s.studentId.toLowerCase().includes(studentSearchQuery.toLowerCase()) ||
            s.college.toLowerCase().includes(studentSearchQuery.toLowerCase()) ||
            s.targetInstitution.toLowerCase().includes(studentSearchQuery.toLowerCase())
           )
           .map(s => s.studentId);

          if (selectedStudentRosterIds.length === filtered.length && filtered.length > 0) {
           setSelectedStudentRosterIds([]);
          } else {
           setSelectedStudentRosterIds(filtered);
          }
         }}
                  style={{
          padding: '8px 14px',
          borderRadius: '12px',
          background: 'rgba(255, 255, 255, 0.08)',
          border: '1px solid rgba(255, 255, 255, 0.2)',
          color: '#fff',
          fontSize: 12.5,
          fontWeight: 700,
          cursor: 'pointer'
         }}
        >
         {selectedStudentRosterIds.length > 0 ? 'Deselect All' : 'Select All'}
        </button>

        <button
         onClick={handlePurgeGhostStudents}
         disabled={isSyncing}
                  style={{
          padding: '8px 16px',
          borderRadius: '12px',
          background: 'rgba(255, 171, 0, 0.15)',
          border: '1px solid rgba(255, 171, 0, 0.35)',
          color: '#ffb300',
          fontSize: 12.5,
          fontWeight: 800,
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: 6
         }}
        >
         <Sparkles size={14} />
         Clean Ghost Duplicates {detectedGhostCount > 0 ? `(${detectedGhostCount})` : ''}
        </button>

        <button
         onClick={fetchStudents}
         disabled={isLoadingStudents}
                  style={{
          padding: '8px 16px',
          borderRadius: '12px',
          background: 'rgba(208, 188, 255, 0.15)',
          border: '1px solid rgba(208, 188, 255, 0.3)',
          color: '#d0bcff',
          fontSize: 12.5,
          fontWeight: 800,
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: 6
         }}
        >
         <RefreshCw size={14} className={isLoadingStudents ? 'spin-icon' : ''} />
         Refresh Telemetry
        </button>
       </div>
      </div>

      {/* Multi-Student Selection Batch Actions Bar */}
      {selectedStudentRosterIds.length > 0 && (
       <div
        className="haze-card"
                style={{
         borderRadius: '16px',
         padding: '14px 20px',
         background: 'linear-gradient(135deg, rgba(208, 188, 255, 0.15) 0%, rgba(0, 229, 255, 0.1) 100%)',
         border: '1px solid rgba(208, 188, 255, 0.35)',
         display: 'flex',
         alignItems: 'center',
         justifyContent: 'space-between',
         flexWrap: 'wrap',
         gap: 12,
         boxShadow: '0 4px 20px rgba(0, 0, 0, 0.3)'
        }}
       >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span style={{ fontSize: 13.5, fontWeight: 900, color: '#fff' }}>
           {selectedStudentRosterIds.length} Candidate(s) Selected
         </span>
        </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
         <button
          onClick={() => setIsMassAssignModalOpen(true)}
                    style={{
           padding: '8px 16px',
           borderRadius: '10px',
           background: 'linear-gradient(135deg, #d0bcff, #b69df8)',
           border: 'none',
           color: '#381e72',
           fontSize: 12.5,
           fontWeight: 900,
           cursor: 'pointer',
           display: 'flex',
           alignItems: 'center',
           gap: 6
          }}
         >
          <BookOpen size={14} />
          <span>Assign Preset Track / Syllabus </span>
         </button>

         <button
          onClick={() => handleSendTargetedAppUpdate(selectedStudentRosterIds)}
          disabled={isSyncing || !appUpdateApkUrl.trim()}
          title="Send test app update to all selected students"
                    style={{
           padding: '8px 16px',
           borderRadius: '10px',
           background: 'rgba(0, 229, 255, 0.15)',
           border: '1px solid rgba(0, 229, 255, 0.4)',
           color: '#00e5ff',
           fontSize: 12.5,
           fontWeight: 800,
           cursor: isSyncing ? 'default' : 'pointer',
           display: 'flex',
           alignItems: 'center',
           gap: 6
          }}
         >
          <Rocket size={14} />
          <span>Send Test Update </span>
         </button>

         <button
          onClick={() => handleMassResetStudentsToGlobal(selectedStudentRosterIds)}
          disabled={isSyncing}
          title="Revert all selected candidates to global default syllabus and routine"
                    style={{
           padding: '8px 14px',
           borderRadius: '10px',
           background: 'rgba(239, 83, 80, 0.15)',
           border: '1px solid rgba(239, 83, 80, 0.35)',
           color: '#ef5350',
           fontSize: 12,
           fontWeight: 800,
           cursor: 'pointer',
           display: 'flex',
           alignItems: 'center',
           gap: 6
          }}
         >
          <RotateCcw size={13} />
          <span>Reset to Global Master</span>
         </button>

         <button
          onClick={() => setSelectedStudentRosterIds([])}
                    style={{
           padding: '8px 12px',
           borderRadius: '10px',
           background: 'rgba(255, 255, 255, 0.08)',
           border: '1px solid rgba(255, 255, 255, 0.15)',
           color: 'rgba(255, 255, 255, 0.7)',
           fontSize: 12,
           fontWeight: 700,
           cursor: 'pointer'
          }}
         >
          Clear Selection
         </button>
        </div>
       </div>
      )}

      {/* Student Grid */}
      {studentsList.length === 0 ? (
              <div className="haze-card" style={{ padding: '40px 20px', textAlign: 'center', borderRadius: '20px' }}>
                <Users size={40} color="rgba(208, 188, 255, 0.4)" style={{ margin: '0 auto 12px' }} />
                <h3 style={{ margin: 0, fontSize: 16, color: '#fff', fontWeight: 800 }}>No Students Logged In Yet</h3>
                <p style={{ margin: '6px 0 0', fontSize: 13, color: 'rgba(255,255,255,0.6)' }}>
         When students open the EAP Tracker Android app, their study metrics, exam stats and profile will automatically appear here in real time.
        </p>
       </div>
      ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(min(100%, 360px), 1fr))', gap: 16 }}>
        {studentsList
         .filter(s =>
          !studentSearchQuery.trim() ||
          s.name.toLowerCase().includes(studentSearchQuery.toLowerCase()) ||
          s.studentId.toLowerCase().includes(studentSearchQuery.toLowerCase()) ||
          s.college.toLowerCase().includes(studentSearchQuery.toLowerCase()) ||
          s.targetInstitution.toLowerCase().includes(studentSearchQuery.toLowerCase())
         )
         .map((student, sIdx) => {
          const isOnlineRecently = (Date.now() - (student.lastActiveEpoch || 0)) < 15 * 60 * 1000;
          const isSelected = selectedStudentRosterIds.includes(student.studentId);
          const isBetaTester = betaTestedStudentIds.includes(student.studentId);
          return (
           <div
            key={sIdx}
            className="haze-card"
                        style={{
             borderRadius: '20px',
             padding: '18px',
             display: 'flex',
             flexDirection: 'column',
             gap: 14,
             position: 'relative',
             border: isSelected ? '2px solid #d0bcff' : '1px solid rgba(208, 188, 255, 0.15)',
             background: isSelected
              ? 'linear-gradient(135deg, rgba(208, 188, 255, 0.15) 0%, rgba(38, 35, 45, 0.95) 100%)'
              : 'linear-gradient(135deg, rgba(29, 27, 32, 0.9) 0%, rgba(38, 35, 45, 0.85) 100%)'
            }}
           >
            {/* Top Header: Checkbox, Avatar, Name, ID, Target */}
                        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
             <input
              type="checkbox"
              checked={isSelected}
              onChange={(e) => {
               e.stopPropagation();
               setSelectedStudentRosterIds((prev) =>
                prev.includes(student.studentId)
                 ? prev.filter((id) => id !== student.studentId)
                 : [...prev, student.studentId]
               );
              }}
                            style={{ width: 18, height: 18, accentColor: '#d0bcff', cursor: 'pointer' }}
             />

             {student.photoUrl ? (
              <img
               src={student.photoUrl}
               alt={student.name}
                              style={{
                width: 46,
                height: 46,
                borderRadius: '14px',
                objectFit: 'cover',
                border: '2px solid #d0bcff'
               }}
              />
             ) : (
              <div
                              style={{
                width: 46,
                height: 46,
                borderRadius: '14px',
                background: 'rgba(208, 188, 255, 0.15)',
                border: '2px solid rgba(208, 188, 255, 0.3)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 18,
                fontWeight: 900,
                color: '#d0bcff'
               }}
              >
               {student.name ? student.name.charAt(0).toUpperCase() : 'S'}
              </div>
             )}

                          <div style={{ flex: 1, minWidth: 0 }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                              <h4 style={{ margin: 0, fontSize: 15, fontWeight: 900, color: '#fff', letterSpacing: '0.2px' }}>
                {student.name || 'Aspirant'}
               </h4>
                              <span style={{
                fontSize: 10.5,
                fontWeight: 800,
                background: 'rgba(208, 188, 255, 0.2)',
                color: '#d0bcff',
                padding: '2px 6px',
                borderRadius: '6px'
               }}>
                {student.studentId}
               </span>
               {isBetaTester && (
                                <span style={{
                 fontSize: 10,
                 fontWeight: 900,
                 background: 'rgba(0, 229, 255, 0.2)',
                 color: '#00e5ff',
                 border: '1px solid rgba(0, 229, 255, 0.4)',
                 padding: '1px 6px',
                 borderRadius: '6px',
                 display: 'inline-flex',
                 alignItems: 'center',
                 gap: 3
                }}>
                 <Rocket size={10} />
                 Beta Tester
                </span>
               )}
              </div>

                            <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 3, flexWrap: 'wrap' }}>
                              <span style={{ fontSize: 11.5, color: 'rgba(255,255,255,0.7)' }}>
                {student.college} • {student.batch}
               </span>
              </div>
             </div>

             {/* Online Indicator */}
                          <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: 4,
              fontSize: 10.5,
              fontWeight: 700,
              color: isOnlineRecently ? '#4caf50' : 'rgba(255,255,255,0.4)',
              background: isOnlineRecently ? 'rgba(76, 175, 80, 0.15)' : 'rgba(255,255,255,0.06)',
              padding: '3px 7px',
              borderRadius: '8px'
             }}>
                            <span style={{
               width: 6,
               height: 6,
               borderRadius: '50%',
               background: isOnlineRecently ? '#4caf50' : 'rgba(255,255,255,0.4)'
              }} />
              <span>{isOnlineRecently ? 'Online' : 'Offline'}</span>
             </div>
            </div>

            {/* Live Studying Pulsing Badge */}
            {student.isStudying && (
                          <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              padding: '8px 12px',
              borderRadius: '10px',
              background: 'rgba(0, 230, 118, 0.12)',
              border: '1px solid rgba(0, 230, 118, 0.4)',
              boxShadow: '0 0 12px rgba(0, 230, 118, 0.2)'
             }}>
                            <span style={{
               width: 8,
               height: 8,
               borderRadius: '50%',
               background: '#00e676',
               boxShadow: '0 0 8px #00e676',
               display: 'inline-block'
              }} />
              <BookOpen size={14} color="#00e676" />
                            <span style={{ fontSize: 12, fontWeight: 900, color: '#00e676' }}>
               CURRENTLY STUDYING: {student.activeSubject || 'Focus Session'}
              </span>
              {student.sessionStartEpoch ? (
                              <span style={{ fontSize: 11, color: 'rgba(255, 255, 255, 0.6)', marginLeft: 'auto' }}>
                Started {new Date(student.sessionStartEpoch).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
               </span>
              ) : null}
             </div>
            )}

            {/* Target Institution Badge */}
                        <div style={{
             display: 'flex',
             alignItems: 'center',
             justifyContent: 'space-between',
             padding: '8px 12px',
             borderRadius: '10px',
             background: 'rgba(255, 213, 79, 0.08)',
             border: '1px solid rgba(255, 213, 79, 0.2)'
            }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Target size={13} color="#ffd54f" />
                            <span style={{ fontSize: 11, fontWeight: 800, color: '#ffd54f' }}>
               Dream Target: {student.targetInstitution || 'Engineering Admission'}
              </span>
             </div>
             {student.email && (
                            <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.5)' }}>
               {student.email}
              </span>
             )}
            </div>

            {/* Real-time Telemetry Stats Grid */}
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8 }}>
             {/* Daily Study Hours */}
                          <div style={{
              background: 'rgba(255,255,255,0.04)',
              padding: '10px',
              borderRadius: '12px',
              display: 'flex',
              flexDirection: 'column',
              gap: 2
             }}>
                            <span style={{ fontSize: 10.5, fontWeight: 700, color: 'rgba(255,255,255,0.5)' }}>TODAY STUDY</span>
                            <span style={{ fontSize: 15, fontWeight: 900, color: '#00e5ff' }}>
                              {(student.todayStudyHours || 0).toFixed(1)}h <span style={{ fontSize: 11, fontWeight: 600, color: 'rgba(255,255,255,0.4)' }}>/ {student.dailyGoalHours || 8}h</span>
              </span>
             </div>

             {/* Syllabus Covered */}
                          <div style={{
              background: 'rgba(255,255,255,0.04)',
              padding: '10px',
              borderRadius: '12px',
              display: 'flex',
              flexDirection: 'column',
              gap: 2
             }}>
                            <span style={{ fontSize: 10.5, fontWeight: 700, color: 'rgba(255,255,255,0.5)' }}>SYLLABUS</span>
                            <span style={{ fontSize: 15, fontWeight: 900, color: '#81c784' }}>
                              {student.syllabusPct || 0}% <span style={{ fontSize: 11, fontWeight: 600, color: 'rgba(255,255,255,0.4)' }}>({student.completedChapters || 0}/{student.totalChapters || 65})</span>
              </span>
             </div>

             {/* Exam Average */}
                          <div style={{
              background: 'rgba(255,255,255,0.04)',
              padding: '10px',
              borderRadius: '12px',
              display: 'flex',
              flexDirection: 'column',
              gap: 2
             }}>
                            <span style={{ fontSize: 10.5, fontWeight: 700, color: 'rgba(255,255,255,0.5)' }}>EXAM AVG</span>
                            <span style={{ fontSize: 15, fontWeight: 900, color: '#ffb74d' }}>
                              {student.examAveragePct || 0}% <span style={{ fontSize: 11, fontWeight: 600, color: 'rgba(255,255,255,0.4)' }}>({student.totalExamsLogged || 0} exams)</span>
              </span>
             </div>
            </div>

            {/* Device & Sync Info */}
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 11, color: 'rgba(255,255,255,0.45)' }}>
             <span>Device: {student.deviceModel || 'Android'}</span>
             <span>
              {student.lastActiveEpoch ? `Active ${new Date(student.lastActiveEpoch).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}` : ''}
             </span>
            </div>

            {/* Action Buttons */}
                        <div style={{ display: 'flex', gap: 8, marginTop: 4, flexWrap: 'wrap' }}>
             <button
              onClick={() => handleOpenStudentDetail(student)}
                            style={{
               flex: 1.1,
               minWidth: 110,
               padding: '9px 12px',
               borderRadius: '10px',
               background: 'rgba(255, 255, 255, 0.08)',
               border: '1px solid rgba(255, 255, 255, 0.25)',
               color: '#fff',
               fontSize: 12,
               fontWeight: 800,
               cursor: 'pointer',
               display: 'flex',
               alignItems: 'center',
               justifyContent: 'center',
               gap: 6
              }}
             >
              <UserCheck size={13} />
              View Profile & Routine
             </button>

             <button
              onClick={() => {
               setPersonalNoticeStudentId(student.studentId);
               setPersonalNoticeStudentName(student.name);
               setIsPersonalNoticeModalOpen(true);
              }}
                            style={{
               flex: 0.9,
               minWidth: 95,
               padding: '9px 12px',
               borderRadius: '10px',
               background: 'rgba(208, 188, 255, 0.15)',
               border: '1px solid rgba(208, 188, 255, 0.3)',
               color: '#d0bcff',
               fontSize: 12,
               fontWeight: 800,
               cursor: 'pointer',
               display: 'flex',
               alignItems: 'center',
               justifyContent: 'center',
               gap: 6
              }}
             >
              <Send size={13} />
              Notice
             </button>

             <button
              onClick={() => {
               setUploadedRoutineTargetStudentId(student.studentId);
               setUploadedRoutineTargetMode('Individual');
               routineJsonUploadRef.current?.click();
              }}
                            style={{
               flex: 1,
               minWidth: 110,
               padding: '9px 12px',
               borderRadius: '10px',
               background: 'rgba(0, 229, 255, 0.15)',
               border: '1px solid rgba(0, 229, 255, 0.35)',
               color: '#00e5ff',
               fontSize: 12,
               fontWeight: 800,
               cursor: 'pointer',
               display: 'flex',
               alignItems: 'center',
               justifyContent: 'center',
               gap: 6
              }}
             >
              <FileUp size={13} />
              Assign Routine
             </button>

             <button
              onClick={() => handleDeleteStudent(student.studentId)}
              title="Delete Student from Cloud"
                            style={{
               padding: '9px 12px',
               borderRadius: '10px',
               background: 'rgba(239, 83, 80, 0.12)',
               border: '1px solid rgba(239, 83, 80, 0.3)',
               color: '#ef5350',
               cursor: 'pointer',
               display: 'flex',
               alignItems: 'center',
               justifyContent: 'center'
              }}
             >
              <Trash2 size={14} />
             </button>
            </div>
           </div>
          );
         })}
       </div>
      )}
     </div>
    )
   )}

   {/* TAB: NOTIFICATIONS HUB */}
   {activeTab === 'notifications' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
     {/* Header Controls */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
      <div>
              <h2 style={{ margin: 0, fontSize: 20, color: '#fff', fontWeight: 900, display: 'flex', alignItems: 'center', gap: 10 }}>
        <Bell size={22} color="#d0bcff" />
        Notification Hub & Push Center
       </h2>
              <p style={{ margin: '4px 0 0', fontSize: 13, color: 'rgba(255, 255, 255, 0.6)' }}>
        Configure automated routine alerts, dispatch live push notifications, and monitor what each student receives in real time.
       </p>
      </div>

            <div style={{ display: 'flex', gap: 8 }}>
       <button
        onClick={fetchSentNotifications}
                style={{
         padding: '8px 16px',
         borderRadius: '12px',
         background: 'rgba(208, 188, 255, 0.15)',
         border: '1px solid rgba(208, 188, 255, 0.3)',
         color: '#d0bcff',
         fontSize: 12.5,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <RefreshCw size={14} />
        Refresh Sent Log
       </button>
      </div>
     </div>

     {/* Grid Layout: Automated Schedules (Left) + Broadcast Dispatcher (Right) */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 420px), 1fr))', gap: 20 }}>
      {/* SECTION 1: AUTOMATED SCHEDULED NOTIFICATIONS */}
            <div className="haze-card" style={{ borderRadius: '22px', padding: '22px', display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <h3 style={{ margin: 0, fontSize: 16, color: '#fff', fontWeight: 800, display: 'flex', alignItems: 'center', gap: 8 }}>
         <Sparkles size={18} color="#ffd54f" />
         Automated Daily Notification Rules
        </h3>
                <span style={{ fontSize: 11, background: 'rgba(255, 213, 79, 0.15)', color: '#ffd54f', padding: '4px 8px', borderRadius: '8px', fontWeight: 800 }}>
         Active Alarms
        </span>
       </div>

       {/* 1. Morning Routine Alert */}
              <div style={{
        background: 'rgba(255, 255, 255, 0.04)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        borderRadius: '16px',
        padding: '14px',
        display: 'flex',
        flexDirection: 'column',
        gap: 8
       }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Sun size={17} color="#ffd54f" />
                    <span style={{ fontWeight: 800, color: '#fff', fontSize: 13.5 }}>Morning Routine Schedule Alert</span>
         </div>
                  <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer', gap: 6 }}>
          <input
           type="checkbox"
           checked={automatedAlerts.morningRoutine.enabled}
           onChange={(e) => setAutomatedAlerts({
            ...automatedAlerts,
            morningRoutine: { ...automatedAlerts.morningRoutine, enabled: e.target.checked }
           })}
                      style={{ cursor: 'pointer' }}
          />
                    <span style={{ fontSize: 12, color: automatedAlerts.morningRoutine.enabled ? '#00e676' : 'rgba(255,255,255,0.4)', fontWeight: 700 }}>
           {automatedAlerts.morningRoutine.enabled ? 'Enabled' : 'Disabled'}
          </span>
         </label>
        </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.6)' }}>Scheduled Time:</span>
         <input
          type="time"
          value={automatedAlerts.morningRoutine.time}
          onChange={(e) => setAutomatedAlerts({
           ...automatedAlerts,
           morningRoutine: { ...automatedAlerts.morningRoutine, time: e.target.value }
          })}
                    style={{
           background: 'rgba(255, 255, 255, 0.08)',
           border: '1px solid rgba(208, 188, 255, 0.3)',
           color: '#fff',
           padding: '4px 8px',
           borderRadius: '8px',
           fontSize: 12,
           fontWeight: 700
          }}
         />
                  <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.5)', marginLeft: 'auto' }}>
          Includes class subject, room & exam
         </span>
        </div>
       </div>

       {/* 2. Evening Progress & Streak Wrap-up */}
              <div style={{
        background: 'rgba(255, 255, 255, 0.04)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        borderRadius: '16px',
        padding: '14px',
        display: 'flex',
        flexDirection: 'column',
        gap: 8
       }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Moon size={17} color="#90caf9" />
                    <span style={{ fontWeight: 800, color: '#fff', fontSize: 13.5 }}>Evening Progress & Streak Wrap-up</span>
         </div>
                  <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer', gap: 6 }}>
          <input
           type="checkbox"
           checked={automatedAlerts.eveningProgress.enabled}
           onChange={(e) => setAutomatedAlerts({
            ...automatedAlerts,
            eveningProgress: { ...automatedAlerts.eveningProgress, enabled: e.target.checked }
           })}
                      style={{ cursor: 'pointer' }}
          />
                    <span style={{ fontSize: 12, color: automatedAlerts.eveningProgress.enabled ? '#00e676' : 'rgba(255,255,255,0.4)', fontWeight: 700 }}>
           {automatedAlerts.eveningProgress.enabled ? 'Enabled' : 'Disabled'}
          </span>
         </label>
        </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.6)' }}>Scheduled Time:</span>
         <input
          type="time"
          value={automatedAlerts.eveningProgress.time}
          onChange={(e) => setAutomatedAlerts({
           ...automatedAlerts,
           eveningProgress: { ...automatedAlerts.eveningProgress, time: e.target.value }
          })}
                    style={{
           background: 'rgba(255, 255, 255, 0.08)',
           border: '1px solid rgba(208, 188, 255, 0.3)',
           color: '#fff',
           padding: '4px 8px',
           borderRadius: '8px',
           fontSize: 12,
           fontWeight: 700
          }}
         />
                  <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.5)', marginLeft: 'auto' }}>
          Includes today's study hours & streak
         </span>
        </div>
       </div>

       {/* 3. Admission Milestone Countdown */}
              <div style={{
        background: 'rgba(255, 255, 255, 0.04)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        borderRadius: '16px',
        padding: '14px',
        display: 'flex',
        flexDirection: 'column',
        gap: 8
       }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Target size={17} color="#d0bcff" />
                    <span style={{ fontWeight: 800, color: '#fff', fontSize: 13.5 }}>Admission Exam Milestone Countdown</span>
         </div>
                  <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer', gap: 6 }}>
          <input
           type="checkbox"
           checked={automatedAlerts.countdown.enabled}
           onChange={(e) => setAutomatedAlerts({
            ...automatedAlerts,
            countdown: { ...automatedAlerts.countdown, enabled: e.target.checked }
           })}
                      style={{ cursor: 'pointer' }}
          />
                    <span style={{ fontSize: 12, color: automatedAlerts.countdown.enabled ? '#00e676' : 'rgba(255,255,255,0.4)', fontWeight: 700 }}>
           {automatedAlerts.countdown.enabled ? 'Enabled' : 'Disabled'}
          </span>
         </label>
        </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.6)' }}>Scheduled Time:</span>
         <input
          type="time"
          value={automatedAlerts.countdown.time}
          onChange={(e) => setAutomatedAlerts({
           ...automatedAlerts,
           countdown: { ...automatedAlerts.countdown, time: e.target.value }
          })}
                    style={{
           background: 'rgba(255, 255, 255, 0.08)',
           border: '1px solid rgba(208, 188, 255, 0.3)',
           color: '#fff',
           padding: '4px 8px',
           borderRadius: '8px',
           fontSize: 12,
           fontWeight: 700
          }}
         />
                  <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.5)', marginLeft: 'auto' }}>
          Syncs with target countdown date
         </span>
        </div>
       </div>

       {/* 4. Motivational Quotes Interval */}
              <div style={{
        background: 'rgba(255, 255, 255, 0.04)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        borderRadius: '16px',
        padding: '14px',
        display: 'flex',
        flexDirection: 'column',
        gap: 8
       }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Sparkles size={17} color="#ffd54f" />
                    <span style={{ fontWeight: 800, color: '#fff', fontSize: 13.5 }}>Admission Motivation Booster</span>
         </div>
                  <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer', gap: 6 }}>
          <input
           type="checkbox"
           checked={automatedAlerts.quotes.enabled}
           onChange={(e) => setAutomatedAlerts({
            ...automatedAlerts,
            quotes: { ...automatedAlerts.quotes, enabled: e.target.checked }
           })}
                      style={{ cursor: 'pointer' }}
          />
                    <span style={{ fontSize: 12, color: automatedAlerts.quotes.enabled ? '#00e676' : 'rgba(255,255,255,0.4)', fontWeight: 700 }}>
           {automatedAlerts.quotes.enabled ? 'Enabled' : 'Disabled'}
          </span>
         </label>
        </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.6)' }}>Interval:</span>
         <select
          value={automatedAlerts.quotes.intervalHours}
          onChange={(e) => setAutomatedAlerts({
           ...automatedAlerts,
           quotes: { ...automatedAlerts.quotes, intervalHours: parseInt(e.target.value) || 4 }
          })}
                    style={{
           background: 'rgba(255, 255, 255, 0.08)',
           border: '1px solid rgba(208, 188, 255, 0.3)',
           color: '#fff',
           padding: '4px 8px',
           borderRadius: '8px',
           fontSize: 12,
           fontWeight: 700
          }}
         >
                    <option value={1} style={{ background: '#1d1b20' }}>Every 1 Hour</option>
                    <option value={2} style={{ background: '#1d1b20' }}>Every 2 Hours</option>
                    <option value={4} style={{ background: '#1d1b20' }}>Every 4 Hours</option>
                    <option value={6} style={{ background: '#1d1b20' }}>Every 6 Hours</option>
                    <option value={8} style={{ background: '#1d1b20' }}>Every 8 Hours</option>
         </select>
                  <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.5)', marginLeft: 'auto' }}>
          Randomized quote from library
         </span>
        </div>
       </div>

       <button
        onClick={handleSaveAutomatedRules}
        disabled={isSyncing}
                style={{
         padding: '12px',
         borderRadius: '14px',
         background: 'linear-gradient(135deg, #d0bcff 0%, #b69df8 100%)',
         border: 'none',
         color: '#381e72',
         fontSize: 13,
         fontWeight: 900,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         justifyContent: 'center',
         gap: 8,
         boxShadow: '0 4px 14px rgba(208, 188, 255, 0.3)'
        }}
       >
        <CloudUpload size={16} />
        Save Automated Notification Schedules
       </button>
      </div>

      {/* SECTION 2: LIVE BROADCAST & TARGETED PUSH DISPATCHER */}
            <div className="haze-card" style={{ borderRadius: '22px', padding: '22px', display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <h3 style={{ margin: 0, fontSize: 16, color: '#fff', fontWeight: 800, display: 'flex', alignItems: 'center', gap: 8 }}>
         <Send size={18} color="#00e5ff" />
         Dispatch Instant Push Alert
        </h3>
                <span style={{ fontSize: 11, background: 'rgba(0, 229, 255, 0.15)', color: '#00e5ff', padding: '4px 8px', borderRadius: '8px', fontWeight: 800 }}>
         Realtime Broadcast
        </span>
       </div>

       {/* Target Audience & Category Selector */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        <div>
                  <label style={{ display: 'block', fontSize: 11.5, color: 'rgba(255,255,255,0.7)', fontWeight: 700, marginBottom: 4 }}>
          Target Audience
         </label>
         <select
          value={broadcastTargetAudience}
          onChange={(e) => setBroadcastTargetAudience(e.target.value as any)}
                    style={{
           width: '100%',
           padding: '9px 12px',
           borderRadius: '12px',
           background: 'rgba(255, 255, 255, 0.08)',
           border: '1px solid rgba(208, 188, 255, 0.3)',
           color: '#fff',
           fontSize: 12.5,
           fontWeight: 700
          }}
         >
                    <option value="All" style={{ background: '#1d1b20' }}>All Registered Students</option>
                    <option value="HSC 25" style={{ background: '#1d1b20' }}>HSC '25 Batch Students</option>
                    <option value="HSC 26" style={{ background: '#1d1b20' }}>HSC '26 Batch Students</option>
                    <option value="Individual" style={{ background: '#1d1b20' }}>Individual Candidate ID</option>
         </select>
        </div>

        <div>
                  <label style={{ display: 'block', fontSize: 11.5, color: 'rgba(255,255,255,0.7)', fontWeight: 700, marginBottom: 4 }}>
          Alert Category
         </label>
         <select
          value={broadcastCategory}
          onChange={(e) => setBroadcastCategory(e.target.value as any)}
                    style={{
           width: '100%',
           padding: '9px 12px',
           borderRadius: '12px',
           background: 'rgba(255, 255, 255, 0.08)',
           border: '1px solid rgba(208, 188, 255, 0.3)',
           color: '#fff',
           fontSize: 12.5,
           fontWeight: 700
          }}
         >
                    <option value="Academic" style={{ background: '#1d1b20' }}>Academic / Syllabus</option>
                    <option value="Routine" style={{ background: '#1d1b20' }}>Schedule & Class Routine</option>
                    <option value="Exam" style={{ background: '#1d1b20' }}>Exam Notice & Marks</option>
                    <option value="Personal" style={{ background: '#1d1b20' }}>Personal Mentor Message</option>
                    <option value="Emergency" style={{ background: '#1d1b20' }}>Urgent Alert</option>
         </select>
        </div>
       </div>

       {/* Individual Student Selection if Individual */}
       {broadcastTargetAudience === 'Individual' && (
        <div>
                  <label style={{ display: 'block', fontSize: 11.5, color: '#ffd54f', fontWeight: 800, marginBottom: 4 }}>
          Select Candidate / Student ID
         </label>
         <select
          value={broadcastTargetStudentKey}
          onChange={(e) => setBroadcastTargetStudentKey(e.target.value)}
                    style={{
           width: '100%',
           padding: '9px 12px',
           borderRadius: '12px',
           background: 'rgba(255, 213, 79, 0.1)',
           border: '1px solid rgba(255, 213, 79, 0.4)',
           color: '#ffd54f',
           fontSize: 12.5,
           fontWeight: 800
          }}
         >
                    <option value="" style={{ background: '#1d1b20' }}>-- Select Student --</option>
          {studentsList.map((s, idx) => (
                      <option key={idx} value={s.studentId} style={{ background: '#1d1b20' }}>
            {s.studentId} — {s.name} ({s.college || 'Engineering Aspirant'})
           </option>
          ))}
         </select>
        </div>
       )}

       {/* Title Input */}
       <div>
                <label style={{ display: 'block', fontSize: 11.5, color: 'rgba(255,255,255,0.7)', fontWeight: 700, marginBottom: 4 }}>
         Notification Title
        </label>
        <input
         type="text"
         placeholder="e.g. Important Change in Physics Paper-1 Exam Schedule"
         value={broadcastTitle}
         onChange={(e) => setBroadcastTitle(e.target.value)}
                  style={{
          width: '100%',
          padding: '9px 12px',
          borderRadius: '12px',
          background: 'rgba(255, 255, 255, 0.08)',
          border: '1px solid rgba(208, 188, 255, 0.25)',
          color: '#fff',
          fontSize: 13,
          fontWeight: 700
         }}
        />
       </div>

       {/* Body Message */}
       <div>
                <label style={{ display: 'block', fontSize: 11.5, color: 'rgba(255,255,255,0.7)', fontWeight: 700, marginBottom: 4 }}>
         Message Content
        </label>
        <textarea
         rows={3}
         placeholder="Type the message that will pop up on the student's notification shade..."
         value={broadcastMessage}
         onChange={(e) => setBroadcastMessage(e.target.value)}
                  style={{
          width: '100%',
          padding: '10px 12px',
          borderRadius: '12px',
          background: 'rgba(255, 255, 255, 0.08)',
          border: '1px solid rgba(208, 188, 255, 0.25)',
          color: '#fff',
          fontSize: 13,
          resize: 'vertical'
         }}
        />
       </div>

       {/* Action Button & URL (Optional) */}
              <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 0.8fr', gap: 10 }}>
        <div>
                  <label style={{ display: 'block', fontSize: 11.5, color: 'rgba(255,255,255,0.7)', fontWeight: 700, marginBottom: 4 }}>
          Resource URL / Action Link (Optional)
         </label>
         <input
          type="text"
          placeholder="https://drive.google.com/... or question PDF"
          value={broadcastActionUrl}
          onChange={(e) => setBroadcastActionUrl(e.target.value)}
                    style={{
           width: '100%',
           padding: '8px 12px',
           borderRadius: '10px',
           background: 'rgba(255, 255, 255, 0.08)',
           border: '1px solid rgba(208, 188, 255, 0.2)',
           color: '#fff',
           fontSize: 12
          }}
         />
        </div>
        <div>
                  <label style={{ display: 'block', fontSize: 11.5, color: 'rgba(255,255,255,0.7)', fontWeight: 700, marginBottom: 4 }}>
          Button Label
         </label>
         <input
          type="text"
          placeholder="e.g. Open Question"
          value={broadcastActionButtonText}
          onChange={(e) => setBroadcastActionButtonText(e.target.value)}
                    style={{
           width: '100%',
           padding: '8px 12px',
           borderRadius: '10px',
           background: 'rgba(255, 255, 255, 0.08)',
           border: '1px solid rgba(208, 188, 255, 0.2)',
           color: '#fff',
           fontSize: 12
          }}
         />
        </div>
       </div>

       {/* Dispatch Button */}
       <button
        onClick={handleDispatchBroadcastNotification}
        disabled={isSyncing || !broadcastMessage.trim()}
                style={{
         padding: '12px',
         borderRadius: '14px',
         background: 'linear-gradient(135deg, #00e5ff 0%, #00b0ff 100%)',
         border: 'none',
         color: '#002633',
         fontSize: 13.5,
         fontWeight: 900,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         justifyContent: 'center',
         gap: 8,
         boxShadow: '0 4px 14px rgba(0, 229, 255, 0.35)',
         marginTop: 4
        }}
       >
        <Send size={16} />
        Dispatch Live Notification Now
       </button>
      </div>
     </div>

     {/* SECTION 2.5: IN-APP AUTO-UPDATE & REMOTE APK RELEASE MANAGER */}
          <div className="haze-card" style={{
      borderRadius: '24px',
      padding: '24px',
      display: 'flex',
      flexDirection: 'column',
      gap: 16,
      background: 'linear-gradient(135deg, rgba(0, 229, 255, 0.05) 0%, rgba(208, 188, 255, 0.05) 100%)',
      border: '1px solid rgba(0, 229, 255, 0.35)',
      boxShadow: '0 8px 32px rgba(0, 229, 255, 0.12)'
     }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div style={{ width: 44, height: 44, borderRadius: 14, background: 'rgba(0, 229, 255, 0.15)', border: '1px solid rgba(0, 229, 255, 0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#00e5ff' }}>
         <Rocket size={22} />
        </div>
        <div>
                  <h3 style={{ margin: 0, fontSize: 17, color: '#fff', fontWeight: 900 }}>
          In-App Auto-Update & Remote APK Release Manager
         </h3>
                  <p style={{ margin: '2px 0 0', fontSize: 12.5, color: 'rgba(255, 255, 255, 0.65)' }}>
          Broadcast new app updates. When students tap the notification, the APK downloads automatically and launches the 1-tap installer!
         </p>
        </div>
       </div>
              <span style={{ fontSize: 11, background: 'rgba(0, 229, 255, 0.2)', color: '#00e5ff', padding: '4px 10px', borderRadius: 8, fontWeight: 800, border: '1px solid rgba(0, 229, 255, 0.4)' }}>
        1-Tap Auto Installer Flow
       </span>
      </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 220px), 1fr))', gap: 14 }}>
       <div>
                <label style={{ fontSize: 11.5, fontWeight: 800, color: 'rgba(255, 255, 255, 0.75)', display: 'block', marginBottom: 5 }}>
         Version Name (e.g. v1.0.1)
        </label>
        <input
         type="text"
         value={appUpdateVersionName}
         onChange={(e) => setAppUpdateVersionName(e.target.value)}
         placeholder="v1.0.1"
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '12px', background: 'rgba(255, 255, 255, 0.08)', border: '1px solid rgba(0, 229, 255, 0.3)', color: '#00e5ff', fontWeight: 800, fontSize: 13 }}
        />
       </div>

       <div>
                <label style={{ fontSize: 11.5, fontWeight: 800, color: 'rgba(255, 255, 255, 0.75)', display: 'block', marginBottom: 5 }}>
         Version Code (Integer Build No)
        </label>
        <input
         type="number"
         value={appUpdateVersionCode}
         onChange={(e) => setAppUpdateVersionCode(parseInt(e.target.value) || 2)}
         placeholder="2"
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '12px', background: 'rgba(255, 255, 255, 0.08)', border: '1px solid rgba(0, 229, 255, 0.3)', color: '#fff', fontWeight: 800, fontSize: 13 }}
        />
       </div>

              <div style={{ gridColumn: 'span 2' }}>
                <label style={{ fontSize: 11.5, fontWeight: 800, color: 'rgba(255, 255, 255, 0.75)', display: 'block', marginBottom: 5 }}>
         Direct APK Download URL (Google Drive direct / GitHub / Firebase / Server)
        </label>
        <input
         type="text"
         value={appUpdateApkUrl}
         onChange={(e) => setAppUpdateApkUrl(e.target.value)}
         placeholder="https://github.com/.../releases/download/v1.0.1/EAPTracker-v1.0.1.apk or direct link"
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '12px', background: 'rgba(255, 255, 255, 0.08)', border: '1px solid rgba(0, 229, 255, 0.4)', color: '#80d8ff', fontSize: 12.5 }}
        />
       </div>
      </div>

      <div>
              <label style={{ fontSize: 11.5, fontWeight: 800, color: 'rgba(255, 255, 255, 0.75)', display: 'block', marginBottom: 5 }}>
        Update Title
       </label>
       <input
        type="text"
        value={appUpdateTitle}
        onChange={(e) => setAppUpdateTitle(e.target.value)}
        placeholder=" New EAP Tracker Update Available"
                style={{ width: '100%', padding: '10px 12px', borderRadius: '12px', background: 'rgba(255, 255, 255, 0.08)', border: '1px solid rgba(255, 255, 255, 0.15)', color: '#fff', fontWeight: 700, fontSize: 13 }}
       />
      </div>

      <div>
              <label style={{ fontSize: 11.5, fontWeight: 800, color: 'rgba(255, 255, 255, 0.75)', display: 'block', marginBottom: 5 }}>
        Changelog / What's New
       </label>
       <textarea
        rows={3}
        value={appUpdateChangelog}
        onChange={(e) => setAppUpdateChangelog(e.target.value)}
        placeholder="• Major performance update&#10;• New dynamic routine engine"
                style={{ width: '100%', padding: '10px 12px', borderRadius: '12px', background: 'rgba(255, 255, 255, 0.08)', border: '1px solid rgba(255, 255, 255, 0.15)', color: '#fff', fontSize: 12.5, lineHeight: 1.5, resize: 'vertical' }}
       />
      </div>

      {/* Target Audience Mode Selector */}
            <div style={{
       background: 'rgba(0, 0, 0, 0.25)',
       border: '1px solid rgba(0, 229, 255, 0.25)',
       borderRadius: '16px',
       padding: '14px',
       display: 'flex',
       flexDirection: 'column',
       gap: 10
      }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
                <span style={{ fontSize: 12, fontWeight: 800, color: '#00e5ff', display: 'flex', alignItems: 'center', gap: 6 }}>
         <Target size={14} />
         <span>Release Deployment Target:</span>
        </span>
        {betaTestedStudentIds.length > 0 && (
                  <span style={{ fontSize: 11, color: '#81c784', background: 'rgba(129, 199, 132, 0.15)', padding: '2px 8px', borderRadius: 6, fontWeight: 700 }}>
           {betaTestedStudentIds.length} candidate(s) currently on beta test
         </span>
        )}
       </div>

              <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
        <button
         onClick={() => setAppUpdateTargetMode('All')}
                  style={{
          padding: '8px 16px',
          borderRadius: '10px',
          background: appUpdateTargetMode === 'All' ? '#00e5ff' : 'rgba(255, 255, 255, 0.08)',
          color: appUpdateTargetMode === 'All' ? '#002633' : '#fff',
          border: 'none',
          fontWeight: 800,
          fontSize: 12,
          cursor: 'pointer'
         }}
        >
          Broadcast to All Students
        </button>

        <button
         onClick={() => setAppUpdateTargetMode('Single')}
                  style={{
          padding: '8px 16px',
          borderRadius: '10px',
          background: appUpdateTargetMode === 'Single' ? '#00e5ff' : 'rgba(255, 255, 255, 0.08)',
          color: appUpdateTargetMode === 'Single' ? '#002633' : '#fff',
          border: 'none',
          fontWeight: 800,
          fontSize: 12,
          cursor: 'pointer'
         }}
        >
          Single Test Student (Beta)
        </button>

        {selectedStudentRosterIds.length > 0 && (
         <button
          onClick={() => setAppUpdateTargetMode('Selected')}
                    style={{
           padding: '8px 16px',
           borderRadius: '10px',
           background: appUpdateTargetMode === 'Selected' ? '#00e5ff' : 'rgba(255, 255, 255, 0.08)',
           color: appUpdateTargetMode === 'Selected' ? '#002633' : '#fff',
           border: 'none',
           fontWeight: 800,
           fontSize: 12,
           cursor: 'pointer'
          }}
         >
           Selected Roster Students ({selectedStudentRosterIds.length})
         </button>
        )}
       </div>

       {appUpdateTargetMode === 'Single' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <label style={{ fontSize: 11.5, color: '#ffd54f', fontWeight: 800 }}>
          Select Candidate to receive Test Release:
         </label>
         <select
          value={appUpdateTargetStudentId}
          onChange={(e) => setAppUpdateTargetStudentId(e.target.value)}
                    style={{
           padding: '10px 12px',
           borderRadius: '10px',
           background: 'rgba(255, 213, 79, 0.1)',
           border: '1px solid rgba(255, 213, 79, 0.4)',
           color: '#ffd54f',
           fontSize: 12.5,
           fontWeight: 800
          }}
         >
                    <option value="" style={{ background: '#1d1b20' }}>-- Choose Test Student --</option>
          {studentsList.map((s, idx) => (
                      <option key={idx} value={s.studentId} style={{ background: '#1d1b20' }}>
            {s.studentId} — {s.name} ({s.college || 'Engineering Aspirant'})
           </option>
          ))}
         </select>
        </div>
       )}
      </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12, marginTop: 4 }}>
              <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', fontSize: 12.5, color: 'rgba(255, 255, 255, 0.85)', fontWeight: 700 }}>
        <input
         type="checkbox"
         checked={appUpdateIsForce}
         onChange={(e) => setAppUpdateIsForce(e.target.checked)}
                  style={{ width: 16, height: 16, accentColor: '#ef5350' }}
        />
        Force Update (Require update before student can use app)
       </label>

       {appUpdateTargetMode === 'All' ? (
        <button
         onClick={handleBroadcastAppUpdate}
         disabled={isSyncing || !appUpdateApkUrl.trim()}
                  style={{
          padding: '12px 24px',
          borderRadius: '14px',
          background: 'linear-gradient(135deg, #00e5ff 0%, #00b0ff 100%)',
          border: 'none',
          color: '#002633',
          fontSize: 13.5,
          fontWeight: 900,
          cursor: isSyncing ? 'default' : 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          boxShadow: '0 4px 16px rgba(0, 229, 255, 0.4)'
         }}
        >
         <Rocket size={17} />
         <span>{isSyncing ? 'Broadcasting Update...' : 'Broadcast App Update to All Students '}</span>
        </button>
       ) : appUpdateTargetMode === 'Single' ? (
        <button
         onClick={() => handleSendTargetedAppUpdate([appUpdateTargetStudentId])}
         disabled={isSyncing || !appUpdateApkUrl.trim() || !appUpdateTargetStudentId}
                  style={{
          padding: '12px 24px',
          borderRadius: '14px',
          background: 'linear-gradient(135deg, #ffd54f 0%, #ffb300 100%)',
          border: 'none',
          color: '#3e2723',
          fontSize: 13.5,
          fontWeight: 900,
          cursor: (isSyncing || !appUpdateTargetStudentId) ? 'default' : 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          boxShadow: '0 4px 16px rgba(255, 213, 79, 0.3)'
         }}
        >
         <Rocket size={17} />
         <span>{isSyncing ? 'Deploying...' : 'Deploy Test Update to Student '}</span>
        </button>
       ) : (
        <button
         onClick={() => handleSendTargetedAppUpdate(selectedStudentRosterIds)}
         disabled={isSyncing || !appUpdateApkUrl.trim() || selectedStudentRosterIds.length === 0}
                  style={{
          padding: '12px 24px',
          borderRadius: '14px',
          background: 'linear-gradient(135deg, #ffd54f 0%, #ffb300 100%)',
          border: 'none',
          color: '#3e2723',
          fontSize: 13.5,
          fontWeight: 900,
          cursor: isSyncing ? 'default' : 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          boxShadow: '0 4px 16px rgba(255, 213, 79, 0.3)'
         }}
        >
         <Rocket size={17} />
         <span>{isSyncing ? 'Deploying...' : `Deploy Test Update to Selected (${selectedStudentRosterIds.length}) `}</span>
        </button>
       )}
      </div>
     </div>

     {/* SECTION 3: LIVE STUDENT INBOX AUDITOR & SENT NOTIFICATIONS LOG */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 420px), 1fr))', gap: 20 }}>
      {/* Student Live Inbox Inspector */}
            <div className="haze-card" style={{ borderRadius: '22px', padding: '22px', display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <h3 style={{ margin: 0, fontSize: 16, color: '#fff', fontWeight: 800, display: 'flex', alignItems: 'center', gap: 8 }}>
         <UserCheck size={18} color="#d0bcff" />
         Live Student Notification Inbox Auditor
        </h3>
       </div>
              <p style={{ margin: 0, fontSize: 12.5, color: 'rgba(255, 255, 255, 0.6)' }}>
        Inspect the exact notification history delivered to any specific candidate to confirm they received your updates.
       </p>

              <div style={{ display: 'flex', gap: 10 }}>
        <select
         value={selectedStudentForInboxAudit}
         onChange={(e) => {
          const sid = e.target.value;
          setSelectedStudentForInboxAudit(sid);
          if (sid) handleAuditStudentInbox(sid);
         }}
                  style={{
          flex: 1,
          padding: '9px 12px',
          borderRadius: '12px',
          background: 'rgba(255, 255, 255, 0.08)',
          border: '1px solid rgba(208, 188, 255, 0.3)',
          color: '#fff',
          fontSize: 12.5,
          fontWeight: 700
         }}
        >
                  <option value="" style={{ background: '#1d1b20' }}>-- Select Student to Audit Inbox --</option>
         {studentsList.map((s, idx) => (
                    <option key={idx} value={s.studentId} style={{ background: '#1d1b20' }}>
           {s.studentId} — {s.name} ({s.college || 'Engineering Aspirant'})
          </option>
         ))}
        </select>

        <button
         onClick={() => handleAuditStudentInbox(selectedStudentForInboxAudit)}
         disabled={!selectedStudentForInboxAudit || isLoadingInbox}
                  style={{
          padding: '9px 16px',
          borderRadius: '12px',
          background: 'rgba(208, 188, 255, 0.15)',
          border: '1px solid rgba(208, 188, 255, 0.3)',
          color: '#d0bcff',
          fontWeight: 800,
          fontSize: 12,
          cursor: 'pointer'
         }}
        >
         Inspect
        </button>
       </div>

       {/* Audited Inbox Output */}
       {selectedStudentForInboxAudit && (
                <div style={{
         background: 'rgba(0, 0, 0, 0.25)',
         border: '1px solid rgba(255, 255, 255, 0.08)',
         borderRadius: '16px',
         padding: '14px',
         display: 'flex',
         flexDirection: 'column',
         gap: 10,
         maxHeight: 280,
         overflowY: 'auto'
        }}>
         {studentAuditedInbox.some((item) => item.type === 'Personal Mentor Notice') && (
                    <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 4 }}>
           <button
            onClick={() => handleClearStudentInbox(selectedStudentForInboxAudit)}
                        style={{
             padding: '6px 12px',
             borderRadius: '8px',
             background: 'rgba(239, 83, 80, 0.15)',
             border: '1px solid rgba(239, 83, 80, 0.35)',
             color: '#ef5350',
             fontSize: 11.5,
             fontWeight: 800,
             cursor: 'pointer',
             display: 'flex',
             alignItems: 'center',
             gap: 5
            }}
           >
            <Trash2 size={12} />
            <span>Clear Personal Notices</span>
           </button>
          </div>
         )}

         {isLoadingInbox ? (
                    <div style={{ textAlign: 'center', padding: '20px', color: '#d0bcff' }}>
                      <RefreshCw size={20} className="spin-icon" style={{ margin: '0 auto 8px' }} />
                      <span style={{ fontSize: 12 }}>Loading candidate notification queue...</span>
          </div>
         ) : studentAuditedInbox.length === 0 ? (
                    <span style={{ fontSize: 12.5, color: 'rgba(255,255,255,0.5)', textAlign: 'center', padding: '16px' }}>
           No active personal or broadcast notices waiting for this student.
          </span>
         ) : (
          studentAuditedInbox.map((item, idx) => (
                      <div key={idx} style={{
            background: 'rgba(255, 255, 255, 0.05)',
            border: '1px solid rgba(208, 188, 255, 0.15)',
            borderRadius: '12px',
            padding: '10px 12px',
            display: 'flex',
            flexDirection: 'column',
            gap: 4
           }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <span style={{ fontSize: 11, fontWeight: 800, color: item.type === 'Personal Mentor Notice' ? '#ffd54f' : '#d0bcff' }}>{item.type || 'Notice'}</span>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <button
               onClick={() => {
                showCustomConfirm(
                 'Delete Notification?',
                 `Are you sure you want to delete "${item.title}"?`,
                 async () => {
                  setIsSyncing(true);
                  try {
                   if (item.type === 'Personal Mentor Notice') {
                    const cleanKey = cleanStudentKey(selectedStudentForInboxAudit);
                    await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/students/${cleanKey}/personal_notice.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
                     method: 'DELETE'
                    });
                   } else {
                    await fetch(`https://eap-tracker-default-rtdb.firebaseio.com/global_notifications/latest.json${apiKey.trim() ? `?auth=${apiKey.trim()}` : ''}`, {
                     method: 'DELETE'
                    });
                   }
                   setStudentAuditedInbox((prev) => prev.filter((_, i) => i !== idx));
                   sound.playSuccess();
                   showNotification('Notification deleted successfully');
                  } catch (_e) {
                   sound.playNegative();
                   showNotification('Failed to delete notification', 'error');
                  } finally {
                   setIsSyncing(false);
                  }
                 },
                 { confirmText: 'Delete', cancelText: 'Cancel', isDanger: true }
                );
               }}
               title="Delete this notice"
                              style={{
                background: 'transparent',
                border: 'none',
                color: '#ef5350',
                cursor: 'pointer',
                padding: 2,
                display: 'flex',
                alignItems: 'center'
               }}
              >
               <Trash2 size={13} />
              </button>
             </div>
            </div>
                        <span style={{ fontSize: 12.5, fontWeight: 800, color: '#fff' }}>{item.title}</span>
                        <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.7)' }}>{item.message}</span>
            {item.actionUrl && (
             <a
              href={item.actionUrl}
              target="_blank"
              rel="noopener noreferrer"
                            style={{ fontSize: 11, color: '#00e5ff', textDecoration: 'underline', marginTop: 2, display: 'inline-flex', alignItems: 'center', gap: 4 }}
             >
              <ExternalLink size={12} />
              <span>{item.actionButtonText || 'Open Resource'}</span>
             </a>
            )}
           </div>
          ))
         )}
        </div>
       )}
      </div>

      {/* Sent Notifications History & Delete/Recall */}
            <div className="haze-card" style={{ borderRadius: '22px', padding: '22px', display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <h3 style={{ margin: 0, fontSize: 16, color: '#fff', fontWeight: 800, display: 'flex', alignItems: 'center', gap: 8 }}>
         <FileText size={18} color="#81c784" />
         Sent Notification History Log ({sentNotificationsHistory.length})
        </h3>
       </div>

       {sentNotificationsHistory.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '30px 10px', color: 'rgba(255,255,255,0.4)', fontSize: 13 }}>
         No past notifications logged yet. Dispatched alerts will appear here.
        </div>
       ) : (
                <div style={{
         display: 'flex',
         flexDirection: 'column',
         gap: 10,
         maxHeight: 340,
         overflowY: 'auto'
        }}>
         {sentNotificationsHistory.map((item, idx) => (
                    <div key={idx} style={{
           background: 'rgba(255, 255, 255, 0.04)',
           border: '1px solid rgba(255, 255, 255, 0.1)',
           borderRadius: '14px',
           padding: '12px 14px',
           display: 'flex',
           justifyContent: 'space-between',
           alignItems: 'flex-start',
           gap: 10
          }}>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 4, flex: 1 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          <span style={{
              fontSize: 10.5,
              fontWeight: 800,
              padding: '2px 6px',
              borderRadius: '6px',
              background: item.targetAudience === 'Individual' ? 'rgba(255, 213, 79, 0.2)' : 'rgba(0, 229, 255, 0.15)',
              color: item.targetAudience === 'Individual' ? '#ffd54f' : '#00e5ff'
             }}>
              {item.targetAudience === 'Individual' ? (item.targetStudentId || 'Single') : item.targetAudience}
             </span>
                          <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.4)' }}>
              {item.timestamp ? new Date(item.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', month: 'short', day: 'numeric' }) : ''}
             </span>
            </div>
                        <span style={{ fontSize: 13, fontWeight: 800, color: '#fff' }}>{item.title}</span>
                        <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.7)' }}>{item.message}</span>
           </div>

           <button
            onClick={() => handleDeleteSentNotification(item.id)}
            title="Delete from Log"
                        style={{
             padding: '6px',
             borderRadius: '8px',
             background: 'rgba(239, 83, 80, 0.12)',
             border: '1px solid rgba(239, 83, 80, 0.3)',
             color: '#ef5350',
             cursor: 'pointer',
             display: 'flex',
             alignItems: 'center',
             justifyContent: 'center'
            }}
           >
            <Trash2 size={13} />
           </button>
          </div>
         ))}
        </div>
       )}
      </div>
     </div>
    </div>
   )}

   {/* TAB 2: EXAMS MANAGER */}
   {activeTab === 'exams' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
            <div style={{ display: 'flex', gap: 6 }}>
       {(['Offline', 'Online'] as const).map((mode) => (
        <div
         key={mode}
         onClick={() => setExamMode(mode)}
                  style={{
          padding: '8px 18px',
          borderRadius: '12px',
          background: examMode === mode ? '#d0bcff' : 'rgba(255, 255, 255, 0.08)',
          color: examMode === mode ? '#381e72' : '#fff',
          fontWeight: 800,
          fontSize: 13,
          cursor: 'pointer'
         }}
        >
         {mode} Exams ({mode === 'Offline' ? offlineExams.length : onlineExams.length})
        </div>
       ))}
      </div>

            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
       {/* Push Exam Mode Live to Cloud */}
       <button
        onClick={() => handlePublishModule(examMode === 'Offline' ? 'offlineExams' : 'onlineExams')}
        disabled={isSyncing}
        title={`Push only ${examMode} exams live to Firebase RTDB for all students`}
                style={{
         padding: '8px 15px',
         borderRadius: '12px',
         background: 'linear-gradient(135deg, rgba(255, 183, 77, 0.3), rgba(255, 112, 67, 0.25))',
         border: '1px solid rgba(255, 183, 77, 0.5)',
         color: '#ffb74d',
         fontSize: 12,
         fontWeight: 800,
         cursor: isSyncing ? 'default' : 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6,
         boxShadow: '0 2px 10px rgba(255, 183, 77, 0.2)'
        }}
       >
        <Zap size={14} />
        <span>{isSyncing ? 'Pushing...' : `Push ${examMode} Live`}</span>
       </button>

       {/* Export Current Exam Mode */}
       <button
        onClick={() => examMode === 'Offline' ? handleExportOfflineExamsJSON() : handleExportOnlineExamsJSON()}
        title={`Download ${examMode} Exam Schedule (.json)`}
                style={{
         padding: '8px 14px',
         borderRadius: '12px',
         background: 'rgba(208, 188, 255, 0.16)',
         border: '1px solid rgba(208, 188, 255, 0.35)',
         color: '#d0bcff',
         fontSize: 12,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <Download size={14} />
        Export {examMode}
       </button>

       {/* Export All Exams (Offline + Online) */}
       <button
        onClick={handleExportAllExamsJSON}
        title="Download Both Offline & Online Exam Schedules (.json)"
                style={{
         padding: '8px 14px',
         borderRadius: '12px',
         background: 'rgba(208, 188, 255, 0.1)',
         border: '1px solid rgba(208, 188, 255, 0.25)',
         color: '#d0bcff',
         fontSize: 12,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <Layers size={14} />
        Export All
       </button>

       {/* Save Exams as Reusable Preset */}
       <button
        onClick={() => {
         const activeExams = examMode === 'Offline' ? offlineExams : onlineExams;
         showCustomPrompt(
          `Save Active ${examMode} Exams as Preset Track`,
          (name) => {
           if (name && name.trim()) {
            handleSavePresetFromData(
             name.trim(),
             'Exams',
             { exams: activeExams },
             `Custom ${examMode} exam schedule created by mentor`
            );
           }
          },
          { placeholder: 'e.g. 20-Day Model Test Series' }
         );
        }}
        title="Save current exam schedule as a reusable track preset"
                style={{
         padding: '8px 14px',
         borderRadius: '12px',
         background: 'rgba(255, 213, 79, 0.15)',
         border: '1px solid rgba(255, 213, 79, 0.4)',
         color: '#ffd54f',
         fontSize: 12,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <Star size={14} />
        <span>Save as Preset</span>
       </button>

       {/* Presets & Track Studio Link */}
       <button
        onClick={() => setActiveTabWithRoute('presets')}
        title="Open Dedicated Presets & Track Studio"
                style={{
         padding: '8px 14px',
         borderRadius: '12px',
         background: 'linear-gradient(135deg, rgba(208, 188, 255, 0.25), rgba(182, 157, 248, 0.2))',
         border: '1px solid rgba(208, 188, 255, 0.4)',
         color: '#d0bcff',
         fontSize: 12,
         fontWeight: 900,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <Sparkles size={14} />
        <span>Presets Studio ({presetsList.length})</span>
       </button>

       {/* Hidden file input for exam json upload */}
       <input
        type="file"
        ref={examJsonUploadRef}
        accept=".json,application/json"
                style={{ display: 'none' }}
        onChange={handleImportJSON}
       />
       <button
        onClick={() => examJsonUploadRef.current?.click()}
        title="Upload Exam Schedule JSON"
                style={{
         padding: '8px 14px',
         borderRadius: '12px',
         background: 'rgba(0, 229, 255, 0.15)',
         border: '1px solid rgba(0, 229, 255, 0.35)',
         color: '#00e5ff',
         fontSize: 12,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <FileUp size={14} />
        Import JSON
       </button>

       <button
        onClick={() => setIsAddingExam(true)}
                style={{
         padding: '8px 18px',
         borderRadius: '12px',
         background: '#d0bcff',
         color: '#381e72',
         border: 'none',
         fontSize: 13,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <Plus size={16} />
        Add Schedule
       </button>

       {/* Sort Exam by Date */}
       <button
        onClick={() => {
         const parseDateEpoch = (d: string): number => {
          const months: Record<string, number> = {
           Jan: 0, Feb: 1, Mar: 2, Apr: 3, May: 4, Jun: 5,
           Jul: 6, Aug: 7, Sep: 8, Oct: 9, Nov: 10, Dec: 11
          };
          const m = d.match(/^(\d{1,2})-([A-Za-z]{3})-(\d{2,4})$/);
          if (!m) return 0;
          const yr = m[3].length === 2 ? 2000 + Number(m[3]) : Number(m[3]);
          return new Date(yr, months[m[2]] ?? 0, Number(m[1])).getTime();
         };
         if (examMode === 'Offline') {
          setOfflineExams(prev => [...prev].sort((a, b) => parseDateEpoch(a.date) - parseDateEpoch(b.date)));
         } else {
          setOnlineExams(prev => [...prev].sort((a, b) => parseDateEpoch(a.date) - parseDateEpoch(b.date)));
         }
         showNotification('Exams sorted by date!');
        }}
                style={{
         padding: '8px 14px',
         borderRadius: '12px',
         background: 'rgba(129, 199, 132, 0.15)',
         border: '1px solid rgba(129, 199, 132, 0.35)',
         color: '#81c784',
         fontSize: 12,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <ArrowUpDown size={13} />
        Sort by Date
       </button>
      </div>
     </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(min(100%, 320px), 1fr))', gap: 14 }}>
      {currentExams.map((item, idx) => (
       <div
        key={idx}
        draggable
        onDragStart={(e) => e.dataTransfer.setData('text/plain', String(idx))}
        onDragOver={(e) => e.preventDefault()}
        onDrop={(e) => {
         e.preventDefault();
         const fromIdx = Number(e.dataTransfer.getData('text/plain'));
         if (fromIdx === idx) return;
         const reorder = (arr: any[]) => {
          const copy = [...arr];
          const [moved] = copy.splice(fromIdx, 1);
          copy.splice(idx, 0, moved);
          return copy;
         };
         if (examMode === 'Offline') setOfflineExams(reorder);
         else setOnlineExams(reorder);
        }}
        className="haze-card"
                style={{
         borderRadius: '18px',
         padding: '16px',
         display: 'flex',
         flexDirection: 'column',
         gap: 10,
         border: '1px solid rgba(255, 255, 255, 0.1)',
         cursor: 'grab',
         transition: 'opacity 0.2s, transform 0.15s'
        }}
       >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {/* Drag handle */}
                    <div title="Drag to reorder" style={{ color: 'rgba(255,255,255,0.3)', cursor: 'grab', display: 'flex', alignItems: 'center' }}>
           <GripVertical size={16} />
          </div>
          <div>
                      <span style={{ fontSize: 16, fontWeight: 900, color: '#fff' }}>{item.date}</span>
                      <span style={{ fontSize: 13, color: '#d0bcff', marginLeft: 8, fontWeight: 700 }}>({item.day})</span>
          </div>
         </div>

                  <div style={{ display: 'flex', gap: 4 }}>
          {/* Push Single Exam Live Button */}
          <button
           onClick={() => handlePublishModule('singleDayExam', { type: examMode, item })}
           disabled={isSyncing}
           title="Push this single exam schedule live to all student devices instantly"
                      style={{
            width: 32,
            height: 32,
            borderRadius: '8px',
            background: 'rgba(255, 183, 77, 0.16)',
            border: '1px solid rgba(255, 183, 77, 0.35)',
            color: '#ffb74d',
            cursor: isSyncing ? 'default' : 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
           }}
          >
           <Zap size={14} />
          </button>

          <button
           onClick={() => setEditingExamItem({ index: idx, item })}
                      style={{
            width: 32,
            height: 32,
            borderRadius: '8px',
            background: 'rgba(255, 255, 255, 0.08)',
            border: 'none',
            color: '#fff',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
           }}
          >
           <Edit2 size={14} />
          </button>
          <button
           onClick={() => {
            showCustomConfirm(
             `Delete Exam on ${item.date}?`,
             `Are you sure you want to delete the ${examMode} exam for ${item.date}? This will also push the update live to student devices.`,
             () => {
              if (examMode === 'Offline') {
               const updated = offlineExams.filter((_, i) => i !== idx);
               setOfflineExams(updated);
               // Directly push updated array (avoids React state closure lag)
               pushDeletedArrayToFirebase('offlineExams', updated);
              } else {
               const updated = onlineExams.filter((_, i) => i !== idx);
               setOnlineExams(updated);
               pushDeletedArrayToFirebase('onlineExams', updated);
              }
              showNotification('Exam deleted & synced to student devices!');
             },
             { confirmText: 'Delete Exam', cancelText: 'Cancel', isDanger: true }
            );
           }}
                      style={{
            width: 32,
            height: 32,
            borderRadius: '8px',
            background: 'rgba(239, 83, 80, 0.15)',
            border: 'none',
            color: '#ef5350',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
           }}
          >
           <Trash2 size={14} />
          </button>
         </div>
        </div>

        {item.exams && item.exams.map((ex: string, eIdx: number) => (
                  <div key={eIdx} style={{ fontSize: 13, fontWeight: 800, color: '#d0bcff' }}>
          • {ex}
         </div>
        ))}

        {item.syllabus && item.syllabus.length > 0 && (
                  <div style={{ fontSize: 12, color: 'rgba(255, 255, 255, 0.8)', background: 'rgba(255,255,255,0.03)', padding: 8, borderRadius: 8 }}>
          <strong>Syllabus:</strong> {item.syllabus.join('; ')}
         </div>
        )}
       </div>
      ))}
     </div>
    </div>
   )}

   {/* TAB 3: QUOTES & DAILY MOTIVATION */}
   {activeTab === 'quotes' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
     {/* Header Card */}
     <div
      className="haze-card"
            style={{
       borderRadius: '24px',
       padding: '24px',
       display: 'flex',
       flexDirection: 'column',
       gap: 12
      }}
     >
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
       <Quote size={22} color="#d0bcff" />
              <h2 style={{ margin: 0, fontSize: 18, fontWeight: 900, color: '#fff' }}>
        Motivational Quotes & Daily Inspiration Hub
       </h2>
      </div>
            <p style={{ margin: 0, fontSize: 13.5, color: 'rgba(255, 255, 255, 0.75)', lineHeight: '20px' }}>
       Broadcast powerful quotes directly to student phone notification bars or set the active daily motivation displayed in their app profiles.
      </p>
     </div>

     {/* Custom Quote Creator Card */}
     <div
      className="haze-card"
            style={{
       borderRadius: '24px',
       padding: '20px',
       display: 'flex',
       flexDirection: 'column',
       gap: 14
      }}
     >
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
       <Plus size={18} color="#d0bcff" />
              <h3 style={{ margin: 0, fontSize: 15, fontWeight: 800, color: '#fff' }}>
        Compose Custom Quote or Instant Message
       </h3>
      </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 240px), 1fr))', gap: 10 }}>
       <input
        type="text"
        placeholder="Type motivational quote (e.g. 'One focused session can change your whole day.')..."
        value={customQuoteText}
        onChange={(e) => setCustomQuoteText(e.target.value)}
                style={{
         padding: '12px 14px',
         borderRadius: '12px',
         background: 'rgba(255, 255, 255, 0.06)',
         border: '1px solid rgba(208, 188, 255, 0.25)',
         color: '#fff',
         fontSize: 13.5
        }}
       />
       <input
        type="text"
        placeholder="Author / Tag (e.g. Mentor)"
        value={customQuoteAuthor}
        onChange={(e) => setCustomQuoteAuthor(e.target.value)}
                style={{
         padding: '12px 14px',
         borderRadius: '12px',
         background: 'rgba(255, 255, 255, 0.06)',
         border: '1px solid rgba(208, 188, 255, 0.25)',
         color: '#fff',
         fontSize: 13.5
        }}
       />
      </div>

            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
       <button
        onClick={handleAddCustomQuote}
                style={{
         padding: '10px 18px',
         borderRadius: '12px',
         background: 'rgba(208, 188, 255, 0.16)',
         border: '1px solid rgba(208, 188, 255, 0.3)',
         color: '#d0bcff',
         fontWeight: 800,
         fontSize: 13,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <Plus size={16} />
        Save to Library
       </button>

       <button
        onClick={() => handleSendDirectNotification("Daily Motivation", customQuoteText, "Motivation")}
        disabled={!customQuoteText.trim() || isSyncing}
                style={{
         padding: '10px 18px',
         borderRadius: '12px',
         background: '#d0bcff',
         color: '#381e72',
         border: 'none',
         fontWeight: 900,
         fontSize: 13,
         cursor: customQuoteText.trim() ? 'pointer' : 'not-allowed',
         display: 'flex',
         alignItems: 'center',
         gap: 6,
         opacity: customQuoteText.trim() ? 1 : 0.5
        }}
       >
        <Send size={16} />
        Send as Instant Notification
       </button>

       <button
        onClick={() => handleSetDailyQuote(customQuoteText, customQuoteAuthor)}
        disabled={!customQuoteText.trim() || isSyncing}
                style={{
         padding: '10px 18px',
         borderRadius: '12px',
         background: 'rgba(129, 199, 132, 0.2)',
         border: '1px solid rgba(129, 199, 132, 0.4)',
         color: '#81c784',
         fontWeight: 800,
         fontSize: 13,
         cursor: customQuoteText.trim() ? 'pointer' : 'not-allowed',
         display: 'flex',
         alignItems: 'center',
         gap: 6,
         opacity: customQuoteText.trim() ? 1 : 0.5
        }}
       >
        <Sparkles size={16} />
        Set as Daily App Quote
       </button>
      </div>
     </div>

     {/* Category Filter Chips */}
          <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 4 }}>
      {[
       { id: 'all', label: 'All Quotes', count: quotesList.length },
       { id: 'top10', label: 'Top 10 Selection', count: quotesList.filter(q => q.category === 'top10').length },
       { id: 'discipline', label: 'Discipline & Consistency', count: quotesList.filter(q => q.category === 'discipline').length },
       { id: 'focus', label: 'Focus & EPA Style', count: quotesList.filter(q => q.category === 'focus').length },
       { id: 'lazy', label: 'Lazy Mode Breakers', count: quotesList.filter(q => q.category === 'lazy').length },
       { id: 'exam', label: 'Exam-Focused', count: quotesList.filter(q => q.category === 'exam').length },
       { id: 'motivational', label: 'Strong Motivational', count: quotesList.filter(q => q.category === 'motivational').length }
      ].map(cat => {
       const isSel = selectedQuoteCategory === cat.id;
       return (
        <button
         key={cat.id}
         onClick={() => {
          sound.playTick();
          setSelectedQuoteCategory(cat.id);
         }}
                  style={{
          padding: '8px 16px',
          borderRadius: '20px',
          background: isSel ? '#d0bcff' : 'rgba(255, 255, 255, 0.06)',
          color: isSel ? '#381e72' : 'rgba(255, 255, 255, 0.8)',
          border: isSel ? 'none' : '1px solid rgba(255, 255, 255, 0.1)',
          fontWeight: 800,
          fontSize: 12.5,
          cursor: 'pointer',
          whiteSpace: 'nowrap',
          display: 'flex',
          alignItems: 'center',
          gap: 6
         }}
        >
         <span>{cat.label}</span>
                  <span style={{
          fontSize: 11,
          background: isSel ? 'rgba(56, 30, 114, 0.2)' : 'rgba(255,255,255,0.1)',
          padding: '2px 6px',
          borderRadius: 10
         }}>
          {cat.count}
         </span>
        </button>
       );
      })}
     </div>

     {/* Quotes Cards Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(min(100%, 300px), 1fr))', gap: 14 }}>
      {quotesList
       .filter(q => selectedQuoteCategory === 'all' || q.category === selectedQuoteCategory)
       .map((q) => (
        <div
         key={q.id}
         className="haze-card"
                  style={{
          borderRadius: '20px',
          padding: '18px',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          gap: 14,
          border: '1px solid rgba(255, 255, 255, 0.08)'
         }}
        >
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
           <span
                        style={{
             fontSize: 10.5,
             fontWeight: 800,
             padding: '3px 8px',
             borderRadius: '8px',
             background: 'rgba(208, 188, 255, 0.15)',
             color: '#d0bcff',
             textTransform: 'uppercase',
             letterSpacing: '0.5px'
            }}
           >
            {q.category}
           </span>
                      <span style={{ fontSize: 11, color: 'rgba(255, 255, 255, 0.5)' }}>
            — {q.author}
           </span>
          </div>

                    <p style={{ margin: 0, fontSize: 14.5, fontWeight: 700, color: '#fff', lineHeight: '22px' }}>
           “{q.quote}”
          </p>
         </div>

         {/* Actions */}
                  <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <button
           onClick={() => handleSendDirectNotification("Daily Motivation", q.quote, "Motivation")}
           title="Send this quote to all student phones now"
                      style={{
            flex: 1,
            padding: '8px 12px',
            borderRadius: '10px',
            background: '#d0bcff',
            color: '#381e72',
            border: 'none',
            fontWeight: 800,
            fontSize: 12,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 6
           }}
          >
           <Send size={13} />
           <span>Send Push</span>
          </button>

          <button
           onClick={() => handleSetDailyQuote(q.quote, q.author)}
           title="Set as the daily quote shown in the app"
                      style={{
            padding: '8px 12px',
            borderRadius: '10px',
            background: 'rgba(129, 199, 132, 0.18)',
            border: '1px solid rgba(129, 199, 132, 0.3)',
            color: '#81c784',
            fontWeight: 800,
            fontSize: 12,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: 6
           }}
          >
           <Sparkles size={13} />
           <span>Set Daily</span>
          </button>

          <button
           onClick={() => {
            navigator.clipboard.writeText(q.quote);
            sound.playSuccess();
            showNotification('Quote copied to clipboard!');
           }}
           title="Copy to clipboard"
                      style={{
            padding: '8px 10px',
            borderRadius: '10px',
            background: 'rgba(255, 255, 255, 0.08)',
            border: '1px solid rgba(255, 255, 255, 0.15)',
            color: '#fff',
            cursor: 'pointer'
           }}
          >
           <Copy size={13} />
          </button>
         </div>
        </div>
       ))}
     </div>
    </div>
   )}

   {/* TAB 4: SYLLABUS & SUBJECTS */}
   {activeTab === 'syllabus' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
     {/* Header & Multi-Select Action Bar */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <span style={{ fontSize: 16, fontWeight: 800, color: '#fff' }}>
        Subjects ({subjects.length}) & Curriculum Parts
       </span>
       {selectedSubjectIndices.length > 0 && (
                <span style={{ fontSize: 12, background: 'rgba(0, 229, 255, 0.2)', color: '#00e5ff', padding: '3px 10px', borderRadius: 8, fontWeight: 800 }}>
         {selectedSubjectIndices.length} Selected
        </span>
       )}
      </div>

            <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
       {selectedSubjectIndices.length > 0 ? (
        <>
         <button
          onClick={() => {
           showCustomConfirm(
            `Delete ${selectedSubjectIndices.length} Selected Subjects?`,
            `Are you sure you want to delete ${selectedSubjectIndices.length} subjects and all their chapters? This action cannot be undone.`,
            () => {
             setSubjects((prev) => prev.filter((_, idx) => !selectedSubjectIndices.includes(idx)));
             setSelectedSubjectIndices([]);
             showNotification(`Deleted ${selectedSubjectIndices.length} subjects.`);
            },
            { confirmText: `Delete (${selectedSubjectIndices.length})`, cancelText: 'Cancel', isDanger: true }
           );
          }}
                    style={{
           padding: '8px 14px',
           borderRadius: '12px',
           background: 'rgba(239, 83, 80, 0.2)',
           color: '#ef5350',
           border: '1px solid rgba(239, 83, 80, 0.4)',
           fontSize: 12.5,
           fontWeight: 800,
           cursor: 'pointer',
           display: 'flex',
           alignItems: 'center',
           gap: 6
          }}
         >
          <Trash2 size={14} />
          <span>Delete Selected ({selectedSubjectIndices.length})</span>
         </button>
         <button
          onClick={() => setSelectedSubjectIndices([])}
                    style={{
           padding: '8px 12px',
           borderRadius: '12px',
           background: 'rgba(255, 255, 255, 0.08)',
           color: '#fff',
           border: '1px solid rgba(255, 255, 255, 0.15)',
           fontSize: 12.5,
           fontWeight: 700,
           cursor: 'pointer'
          }}
         >
          Deselect All
         </button>
        </>
       ) : (
        <button
         onClick={() => setSelectedSubjectIndices(subjects.map((_, idx) => idx))}
                  style={{
          padding: '8px 12px',
          borderRadius: '12px',
          background: 'rgba(255, 255, 255, 0.06)',
          color: 'rgba(255,255,255,0.8)',
          border: '1px solid rgba(255, 255, 255, 0.12)',
          fontSize: 12,
          fontWeight: 700,
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: 6
         }}
        >
         <CheckSquare size={13} />
         <span>Select All</span>
        </button>
       )}

       {/* Save Syllabus as Reusable Preset */}
       <button
        onClick={() => {
         showCustomPrompt(
          'Save Active Syllabus as Preset Track',
          (name) => {
           if (name && name.trim()) {
            handleSavePresetFromData(
             name.trim(),
             'Syllabus',
             { subjects },
             'Custom admission syllabus curriculum created by mentor'
            );
           }
          },
          { placeholder: 'e.g. Engineering Admission Master Track' }
         );
        }}
        title="Save current syllabus curriculum as a reusable track preset"
                style={{
         padding: '8px 14px',
         borderRadius: '12px',
         background: 'rgba(255, 213, 79, 0.15)',
         border: '1px solid rgba(255, 213, 79, 0.4)',
         color: '#ffd54f',
         fontSize: 12,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <Star size={14} />
        <span>Save as Preset</span>
       </button>

       {/* Presets & Track Studio Link */}
       <button
        onClick={() => setActiveTabWithRoute('presets')}
        title="Open Dedicated Presets & Track Studio"
                style={{
         padding: '8px 16px',
         borderRadius: '12px',
         background: 'linear-gradient(135deg, rgba(208, 188, 255, 0.25), rgba(182, 157, 248, 0.2))',
         border: '1px solid rgba(208, 188, 255, 0.4)',
         color: '#d0bcff',
         fontSize: 12.5,
         fontWeight: 900,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <Sparkles size={14} />
        <span>Presets Studio ({presetsList.length})</span>
       </button>

       <button
        onClick={handlePublishToCloud}
        disabled={isSyncing}
                style={{
         padding: '8px 16px',
         borderRadius: '12px',
         background: 'rgba(129, 199, 132, 0.2)',
         color: '#81c784',
         border: '1px solid rgba(129, 199, 132, 0.4)',
         fontSize: 13,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <CloudUpload size={16} />
        {isSyncing ? 'Publishing...' : 'Publish to Apps'}
       </button>

       <button
        onClick={() => {
         showCustomPrompt(
          'Add New Subject',
          (val) => {
           if (!val.trim()) return;
           const newSub: Subject = {
            id: `sub_${Date.now()}`,
            name: val.trim(),
            iconName: 'BookOpen',
            papers: [
             { id: `pap_1_${Date.now()}`, name: '1st Paper', chapters: [] },
             { id: `pap_2_${Date.now()}`, name: '2nd Paper', chapters: [] }
            ]
           };
           setSubjects((prev) => [...prev, newSub]);
           showNotification(`Added subject "${val.trim()}"`);
          },
          { placeholder: 'e.g. ICT, Bangla, English' }
         );
        }}
                style={{
         padding: '8px 18px',
         borderRadius: '12px',
         background: '#d0bcff',
         color: '#381e72',
         border: 'none',
         fontSize: 13,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <Plus size={16} />
        Add Subject
       </button>
      </div>
     </div>

     {/* Subjects List with Drag Handles, Up/Down, Renaming */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      {subjects.map((sub, sIdx) => {
       const isSelected = selectedSubjectIndices.includes(sIdx);
       return (
        <div
         key={sub.id || sIdx}
         className="haze-card"
                  style={{
          borderRadius: '20px',
          padding: '20px',
          display: 'flex',
          flexDirection: 'column',
          gap: 14,
          border: isSelected ? '1.5px solid #00e5ff' : '1px solid rgba(255, 255, 255, 0.08)',
          background: isSelected ? 'rgba(0, 229, 255, 0.04)' : undefined
         }}
        >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
           <input
            type="checkbox"
            checked={isSelected}
            onChange={(e) => {
             if (e.target.checked) {
              setSelectedSubjectIndices((prev) => [...prev, sIdx]);
             } else {
              setSelectedSubjectIndices((prev) => prev.filter((i) => i !== sIdx));
             }
            }}
                        style={{ width: 17, height: 17, accentColor: '#00e5ff', cursor: 'pointer' }}
           />

           {/* Reorder Arrows for Subject */}
                      <div style={{ display: 'flex', gap: 2 }}>
            <button
             onClick={() => moveSubjectUp(sIdx)}
             disabled={sIdx === 0}
             title="Move Subject Up"
                          style={{
              background: sIdx === 0 ? 'rgba(255,255,255,0.02)' : 'rgba(255,255,255,0.08)',
              border: 'none',
              color: sIdx === 0 ? 'rgba(255,255,255,0.2)' : '#fff',
              borderRadius: 6,
              padding: '4px 6px',
              cursor: sIdx === 0 ? 'default' : 'pointer'
             }}
            >
             <ArrowUp size={13} />
            </button>
            <button
             onClick={() => moveSubjectDown(sIdx)}
             disabled={sIdx === subjects.length - 1}
             title="Move Subject Down"
                          style={{
              background: sIdx === subjects.length - 1 ? 'rgba(255,255,255,0.02)' : 'rgba(255,255,255,0.08)',
              border: 'none',
              color: sIdx === subjects.length - 1 ? 'rgba(255,255,255,0.2)' : '#fff',
              borderRadius: 6,
              padding: '4px 6px',
              cursor: sIdx === subjects.length - 1 ? 'default' : 'pointer'
             }}
            >
             <ArrowDown size={13} />
            </button>
           </div>

                      <span style={{ fontSize: 13, fontWeight: 800, color: 'rgba(255,255,255,0.4)', minWidth: 20 }}>#{sIdx + 1}</span>
                      <span style={{ fontSize: 18, fontWeight: 900, color: '#fff' }}>{sub.name}</span>
                      <span style={{ fontSize: 11, background: 'rgba(208, 188, 255, 0.15)', color: '#d0bcff', padding: '2px 8px', borderRadius: 6, fontWeight: 700 }}>
            {sub.papers.reduce((acc, p) => acc + p.chapters.length, 0)} Chapters
           </span>
          </div>

                    <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
           {/* Rename Subject */}
           <button
            onClick={() => {
             showCustomPrompt(
              `Rename Subject "${sub.name}"`,
              (val) => {
               if (!val.trim()) return;
               setSubjects((prev) => prev.map((s, idx) => idx === sIdx ? { ...s, name: val.trim() } : s));
               showNotification(`Subject renamed to "${val.trim()}"`);
              },
              { initialValue: sub.name, placeholder: 'Enter new subject name' }
             );
            }}
                        style={{
             background: 'rgba(208, 188, 255, 0.12)',
             border: 'none',
             color: '#d0bcff',
             padding: '6px 12px',
             borderRadius: '8px',
             cursor: 'pointer',
             fontSize: 12,
             fontWeight: 700,
             display: 'flex',
             alignItems: 'center',
             gap: 5
            }}
           >
            <Edit2 size={13} />
            <span>Rename</span>
           </button>

           {/* Delete Subject */}
           <button
            onClick={() => {
             showCustomConfirm(
              `Delete Subject ${sub.name}?`,
              `Are you sure you want to delete ${sub.name} and all its chapters from the syllabus master list?`,
              () => {
               setSubjects((prev) => prev.filter((_, i) => i !== sIdx));
               showNotification(`Subject "${sub.name}" deleted`);
              },
              { confirmText: 'Delete Subject', cancelText: 'Cancel', isDanger: true }
             );
            }}
                        style={{
             background: 'rgba(239, 83, 80, 0.15)',
             border: 'none',
             color: '#ef5350',
             padding: '6px 12px',
             borderRadius: '8px',
             cursor: 'pointer',
             fontSize: 12,
             fontWeight: 700,
             display: 'flex',
             alignItems: 'center',
             gap: 5
            }}
           >
            <Trash2 size={13} />
            <span>Delete</span>
           </button>
          </div>
         </div>

         {/* Papers */}
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 300px), 1fr))', gap: 12 }}>
          {sub.papers.map((paper, pIdx) => (
           <div
            key={paper.id || pIdx}
                        style={{
             background: 'rgba(255, 255, 255, 0.04)',
             border: '1px solid rgba(255, 255, 255, 0.08)',
             borderRadius: '14px',
             padding: '14px'
            }}
           >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                          <span style={{ fontSize: 14, fontWeight: 800, color: '#d0bcff' }}>
              {paper.name} ({paper.chapters.length} Chaps)
             </span>
             <button
              onClick={() => {
               showCustomPrompt(
                `Add Chapter to ${sub.name} - ${paper.name}`,
                (val) => {
                 if (!val.trim()) return;
                 setSubjects((prev) => {
                  const list = [...prev];
                  const papers = [...list[sIdx].papers];
                  const chapters = [...papers[pIdx].chapters, { id: `ch_${Date.now()}`, name: val.trim(), sections: [] }];
                  papers[pIdx] = { ...papers[pIdx], chapters };
                  list[sIdx] = { ...list[sIdx], papers };
                  return list;
                 });
                 showNotification(`Added chapter "${val.trim()}"`);
                },
                { placeholder: 'Enter chapter title' }
               );
              }}
                            style={{
               background: 'rgba(208, 188, 255, 0.18)',
               border: 'none',
               color: '#d0bcff',
               padding: '4px 10px',
               borderRadius: '8px',
               cursor: 'pointer',
               fontSize: 11,
               fontWeight: 800
              }}
             >
              + Add Chapter
             </button>
            </div>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: 4, maxHeight: 240, overflowY: 'auto' }}>
             {paper.chapters.map((chap, cIdx) => (
              <div
               key={chap.id || cIdx}
                              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '6px 8px',
                background: 'rgba(255,255,255,0.02)',
                borderRadius: 8,
                borderBottom: '1px solid rgba(255,255,255,0.04)',
                fontSize: 12.5,
                color: 'rgba(255, 255, 255, 0.9)'
               }}
              >
                              <div style={{ display: 'flex', alignItems: 'center', gap: 6, flex: 1, minWidth: 0 }}>
                                <div style={{ display: 'flex', gap: 2 }}>
                 <button
                  onClick={() => moveChapterUp(sIdx, pIdx, cIdx)}
                  disabled={cIdx === 0}
                                    style={{
                   background: 'transparent',
                   border: 'none',
                   color: cIdx === 0 ? 'rgba(255,255,255,0.15)' : 'rgba(255,255,255,0.6)',
                   cursor: cIdx === 0 ? 'default' : 'pointer',
                   padding: 2
                  }}
                 >
                  <ArrowUp size={11} />
                 </button>
                 <button
                  onClick={() => moveChapterDown(sIdx, pIdx, cIdx)}
                  disabled={cIdx === paper.chapters.length - 1}
                                    style={{
                   background: 'transparent',
                   border: 'none',
                   color: cIdx === paper.chapters.length - 1 ? 'rgba(255,255,255,0.15)' : 'rgba(255,255,255,0.6)',
                   cursor: cIdx === paper.chapters.length - 1 ? 'default' : 'pointer',
                   padding: 2
                  }}
                 >
                  <ArrowDown size={11} />
                 </button>
                </div>
                                <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.4)', minWidth: 16 }}>{cIdx + 1}.</span>
                                <span style={{ textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>{chap.name}</span>
               </div>

                              <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                <button
                 onClick={() => {
                  showCustomPrompt(
                   'Rename Chapter',
                   (val) => {
                    if (!val.trim()) return;
                    setSubjects((prev) => {
                     const list = [...prev];
                     const papers = [...list[sIdx].papers];
                     const chapters = papers[pIdx].chapters.map((c, i) => i === cIdx ? { ...c, name: val.trim() } : c);
                     papers[pIdx] = { ...papers[pIdx], chapters };
                     list[sIdx] = { ...list[sIdx], papers };
                     return list;
                    });
                    showNotification('Chapter renamed');
                   },
                   { initialValue: chap.name, placeholder: 'Enter new chapter name' }
                  );
                 }}
                 title="Rename Chapter"
                                  style={{
                  background: 'transparent',
                  border: 'none',
                  color: '#d0bcff',
                  cursor: 'pointer',
                  padding: 2,
                  display: 'flex',
                  alignItems: 'center'
                 }}
                >
                 <Edit2 size={11} />
                </button>

                <button
                 onClick={() => {
                  showCustomConfirm(
                   `Delete Chapter "${chap.name}"?`,
                   `Are you sure you want to delete chapter "${chap.name}" from ${sub.name}?`,
                   () => {
                    setSubjects((prev) => {
                     const list = [...prev];
                     const papers = [...list[sIdx].papers];
                     const chapters = papers[pIdx].chapters.filter((_, i) => i !== cIdx);
                     papers[pIdx] = { ...papers[pIdx], chapters };
                     list[sIdx] = { ...list[sIdx], papers };
                     return list;
                    });
                    showNotification(`Deleted chapter "${chap.name}"`);
                   },
                   { confirmText: 'Delete Chapter', cancelText: 'Cancel', isDanger: true }
                  );
                 }}
                 title="Delete Chapter"
                                  style={{
                  background: 'transparent',
                  border: 'none',
                  color: '#ef5350',
                  cursor: 'pointer',
                  padding: 2,
                  display: 'flex',
                  alignItems: 'center'
                 }}
                >
                 <X size={14} />
                </button>
               </div>
              </div>
             ))}
            </div>
           </div>
          ))}
         </div>
        </div>
       );
      })}
     </div>

     {/* Subject Rename Modal */}
     {editingSubjectIndex !== null && (
            <div style={{
       position: 'fixed',
       inset: 0,
       background: 'rgba(0, 0, 0, 0.78)',
       backdropFilter: 'blur(12px)',
       zIndex: 10000,
       display: 'flex',
       alignItems: 'center',
       justifyContent: 'center',
       padding: 20
      }}>
              <div className="haze-card" style={{ maxWidth: 440, width: '100%', borderRadius: 24, padding: 24, display: 'flex', flexDirection: 'column', gap: 16, background: '#1d1b20' }}>
                <h3 style={{ margin: 0, fontSize: 18, color: '#fff', fontWeight: 900 }}>Rename Subject</h3>
        <input
         type="text"
         value={editingSubjectName}
         onChange={(e) => setEditingSubjectName(e.target.value)}
         placeholder="Subject Name"
                  style={{
          padding: '12px 14px',
          borderRadius: 12,
          background: 'rgba(255, 255, 255, 0.08)',
          border: '1px solid rgba(208, 188, 255, 0.3)',
          color: '#fff',
          fontSize: 14,
          fontWeight: 700
         }}
        />
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
         <button
          onClick={() => setEditingSubjectIndex(null)}
                    style={{ padding: '10px 16px', borderRadius: 10, background: 'rgba(255, 255, 255, 0.08)', border: 'none', color: '#fff', fontWeight: 700, cursor: 'pointer' }}
         >
          Cancel
         </button>
         <button
          onClick={() => {
           if (editingSubjectName.trim()) {
            setSubjects((prev) => prev.map((s, idx) => idx === editingSubjectIndex ? { ...s, name: editingSubjectName.trim() } : s));
            setEditingSubjectIndex(null);
            showNotification('Subject renamed successfully!');
           }
          }}
                    style={{ padding: '10px 20px', borderRadius: 10, background: '#00e5ff', border: 'none', color: '#002633', fontWeight: 900, cursor: 'pointer' }}
         >
          Save Changes
         </button>
        </div>
       </div>
      </div>
     )}
    </div>
   )}

   {/* TAB: PRESETS & TRACK STUDIO */}
   {activeTab === 'presets' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
     {/* Header & Quick Action Bar */}
          <div className="haze-card admin-header-responsive" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <div style={{
        width: 44,
        height: 44,
        borderRadius: 14,
        background: 'linear-gradient(135deg, rgba(208, 188, 255, 0.25), rgba(182, 157, 248, 0.2))',
        border: '1px solid rgba(208, 188, 255, 0.4)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: '#d0bcff'
       }}>
        <Sparkles size={22} />
       </div>
       <div>
                <h2 style={{ margin: 0, fontSize: 18, fontWeight: 900, color: '#fff', letterSpacing: '0.2px' }}>
         Reusable Presets & Track Studio ({presetsList.length})
        </h2>
                <p style={{ margin: '2px 0 0', fontSize: 12.5, color: 'rgba(255, 255, 255, 0.65)' }}>
         Create, customize, edit, and assign university tracks, crash routines & exam series to candidates.
        </p>
       </div>
      </div>

            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
       <input
        type="file"
        ref={presetJsonUploadRef}
        accept=".json,application/json"
                style={{ display: 'none' }}
        onChange={handleImportPresetJsonFile}
       />
       <button
        onClick={() => presetJsonUploadRef.current?.click()}
        title="Import Track from JSON File"
                style={{
         padding: '9px 15px',
         borderRadius: '12px',
         background: 'rgba(0, 229, 255, 0.15)',
         border: '1px solid rgba(0, 229, 255, 0.35)',
         color: '#00e5ff',
         fontSize: 12.5,
         fontWeight: 800,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <FileUp size={15} />
        <span>Import JSON Track</span>
       </button>

       <button
        onClick={() => {
         const newTemplate: PresetTemplate = {
          id: `preset_${Date.now()}`,
          name: 'New Custom Admission Track',
          category: 'Syllabus',
          description: 'Custom admission track curriculum created by mentor',
          createdAt: Date.now(),
          data: {
           subjects: JSON.parse(JSON.stringify(initialSubjects))
          }
         };
         setEditingPreset(newTemplate);
         setIsCreatingNewPreset(true);
         setPresetEditorTab('visual');
         setPresetRawJsonText(JSON.stringify(newTemplate.data, null, 2));
         sound.playTick();
        }}
                style={{
         padding: '9px 18px',
         borderRadius: '12px',
         background: 'linear-gradient(135deg, #d0bcff, #b69df8)',
         border: 'none',
         color: '#381e72',
         fontSize: 13,
         fontWeight: 900,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6,
         boxShadow: '0 4px 14px rgba(208, 188, 255, 0.35)'
        }}
       >
        <Plus size={16} />
        <span>+ Create New Preset</span>
       </button>

       <button
        onClick={() => {
         showCustomConfirm(
          'Reset to Default Built-in Tracks?',
          'This will reload the default built-in university preset tracks (BUET, Medical, DU KA, 30-Day Crash, CKRUET).',
          () => {
           setPresetsList(defaultBuiltInPresets);
           localStorage.setItem('eap_admin_presets', JSON.stringify(defaultBuiltInPresets));
           showNotification('Reset to default preset tracks library');
          }
         );
        }}
        title="Reset Library to Standard Tracks"
                style={{
         padding: '9px 12px',
         borderRadius: '12px',
         background: 'rgba(255, 255, 255, 0.06)',
         border: '1px solid rgba(255, 255, 255, 0.12)',
         color: 'rgba(255, 255, 255, 0.7)',
         fontSize: 12,
         fontWeight: 700,
         cursor: 'pointer'
        }}
       >
        Reset Defaults
       </button>
      </div>
     </div>

     {/* If Editing a Preset -> Full Visual Customizer / Editor */}
     {editingPreset ? (
            <div className="haze-card" style={{
       borderRadius: '24px',
       padding: '24px',
       display: 'flex',
       flexDirection: 'column',
       gap: 18,
       border: '1.5px solid rgba(208, 188, 255, 0.4)',
       background: 'linear-gradient(135deg, rgba(29, 27, 32, 0.98) 0%, rgba(38, 35, 45, 0.95) 100%)',
       boxShadow: '0 12px 40px rgba(0, 0, 0, 0.5)'
      }}>
       {/* Editor Header */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12, borderBottom: '1px solid rgba(255, 255, 255, 0.1)', paddingBottom: 16 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
         <Edit3 size={20} color="#d0bcff" />
                  <h3 style={{ margin: 0, fontSize: 17, fontWeight: 900, color: '#fff' }}>
          {isCreatingNewPreset ? 'Create New Preset Track' : `Editing Preset: ${editingPreset.name}`}
         </h3>
        </div>

                <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                  <div style={{ display: 'flex', background: 'rgba(255, 255, 255, 0.08)', borderRadius: '10px', padding: 3 }}>
          <button
           onClick={() => setPresetEditorTab('visual')}
                      style={{
            padding: '6px 14px',
            borderRadius: '8px',
            background: presetEditorTab === 'visual' ? '#d0bcff' : 'transparent',
            color: presetEditorTab === 'visual' ? '#381e72' : '#fff',
            border: 'none',
            fontSize: 12,
            fontWeight: 800,
            cursor: 'pointer'
           }}
          >
            Visual Editor
          </button>
          <button
           onClick={() => {
            setPresetRawJsonText(JSON.stringify(editingPreset.data, null, 2));
            setPresetEditorTab('json');
           }}
                      style={{
            padding: '6px 14px',
            borderRadius: '8px',
            background: presetEditorTab === 'json' ? '#00e5ff' : 'transparent',
            color: presetEditorTab === 'json' ? '#002633' : '#fff',
            border: 'none',
            fontSize: 12,
            fontWeight: 800,
            cursor: 'pointer'
           }}
          >
            Raw JSON
          </button>
         </div>

         <button
          onClick={() => {
           setEditingPreset(null);
           setIsCreatingNewPreset(false);
          }}
                    style={{
           padding: '8px 14px',
           borderRadius: '10px',
           background: 'rgba(255, 255, 255, 0.08)',
           border: '1px solid rgba(255, 255, 255, 0.15)',
           color: '#fff',
           fontSize: 12.5,
           fontWeight: 700,
           cursor: 'pointer'
          }}
         >
          Cancel
         </button>

         <button
          onClick={() => {
           if (!editingPreset.name.trim()) {
            showNotification('Please enter a preset name', 'error');
            return;
           }
           let finalData = editingPreset.data;
           if (presetEditorTab === 'json') {
            try {
             finalData = JSON.parse(presetRawJsonText);
            } catch (err: any) {
             showNotification(`Invalid JSON: ${err.message}`, 'error');
             return;
            }
           }
           const updatedPreset = { ...editingPreset, data: finalData };
           setPresetsList((prev) => {
            const exists = prev.some(p => p.id === updatedPreset.id);
            const list = exists ? prev.map(p => p.id === updatedPreset.id ? updatedPreset : p) : [updatedPreset, ...prev];
            localStorage.setItem('eap_admin_presets', JSON.stringify(list));
            return list;
           });
           setEditingPreset(null);
           setIsCreatingNewPreset(false);
           sound.playSuccess();
           showNotification(`Preset track "${updatedPreset.name}" saved!`);
          }}
                    style={{
           padding: '8px 20px',
           borderRadius: '10px',
           background: 'linear-gradient(135deg, #d0bcff, #b69df8)',
           border: 'none',
           color: '#381e72',
           fontSize: 13,
           fontWeight: 900,
           cursor: 'pointer',
           display: 'flex',
           alignItems: 'center',
           gap: 6,
           boxShadow: '0 4px 14px rgba(208, 188, 255, 0.35)'
          }}
         >
          <Check size={16} />
          <span>Save Preset</span>
         </button>
        </div>
       </div>

       {/* Preset Meta Fields */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 220px), 1fr))', gap: 14 }}>
        <div>
                  <label style={{ fontSize: 11.5, fontWeight: 800, color: '#d0bcff', display: 'block', marginBottom: 5 }}>
          Preset Track Name
         </label>
         <input
          type="text"
          value={editingPreset.name}
          onChange={(e) => setEditingPreset({ ...editingPreset, name: e.target.value })}
          placeholder="e.g. Medical Admission Crash Course"
                    style={{ width: '100%', padding: '10px 12px', borderRadius: '12px', background: 'rgba(255, 255, 255, 0.08)', border: '1px solid rgba(208, 188, 255, 0.3)', color: '#fff', fontSize: 13.5, fontWeight: 800 }}
         />
        </div>

        <div>
                  <label style={{ fontSize: 11.5, fontWeight: 800, color: '#d0bcff', display: 'block', marginBottom: 5 }}>
          Category
         </label>
         <select
          value={editingPreset.category}
          onChange={(e) => {
           const cat = e.target.value as any;
           let d = editingPreset.data;
           if (cat === 'Syllabus' && !d.subjects) d = { ...d, subjects: JSON.parse(JSON.stringify(initialSubjects)) };
           if (cat === 'Routine' && !d.routineDays) d = { ...d, routineDays: JSON.parse(JSON.stringify(initialRoutines.slice(0, 30))), routineMode: 'Offline' };
           if (cat === 'Exams' && !d.exams) d = { ...d, exams: JSON.parse(JSON.stringify(masterEapData.offlineExams?.slice(0, 20) || [])) };
           setEditingPreset({ ...editingPreset, category: cat, data: d });
          }}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: '12px', background: '#2b2930', border: '1px solid rgba(208, 188, 255, 0.3)', color: '#fff', fontSize: 13.5, fontWeight: 800 }}
         >
          <option value="Syllabus"> Syllabus Track</option>
          <option value="Routine"> Routine Schedule</option>
          <option value="Exams"> Exam Series</option>
          <option value="Bundle"> Full Track Bundle (Syllabus + Routine + Exams)</option>
         </select>
        </div>

                <div style={{ gridColumn: 'span 2' }}>
                  <label style={{ fontSize: 11.5, fontWeight: 800, color: '#d0bcff', display: 'block', marginBottom: 5 }}>
          Description / Target Focus
         </label>
         <input
          type="text"
          value={editingPreset.description}
          onChange={(e) => setEditingPreset({ ...editingPreset, description: e.target.value })}
          placeholder="Brief summary of target universities, subjects and test schedule..."
                    style={{ width: '100%', padding: '10px 12px', borderRadius: '12px', background: 'rgba(255, 255, 255, 0.08)', border: '1px solid rgba(255, 255, 255, 0.15)', color: 'rgba(255, 255, 255, 0.9)', fontSize: 13 }}
         />
        </div>
       </div>

       {/* Editor Tab Content */}
       {presetEditorTab === 'json' ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: 12, color: 'rgba(255, 255, 255, 0.6)' }}>Edit raw JSON data structure directly:</span>
          <button
           onClick={() => {
            try {
             const p = JSON.parse(presetRawJsonText);
             setPresetRawJsonText(JSON.stringify(p, null, 2));
             showNotification('JSON formatted cleanly');
            } catch (err: any) {
             showNotification(`JSON format error: ${err.message}`, 'error');
            }
           }}
                      style={{ padding: '4px 10px', borderRadius: 8, background: 'rgba(0, 229, 255, 0.15)', border: 'none', color: '#00e5ff', fontSize: 11, fontWeight: 800, cursor: 'pointer' }}
          >
           Format / Beautify
          </button>
         </div>
         <textarea
          value={presetRawJsonText}
          onChange={(e) => setPresetRawJsonText(e.target.value)}
          rows={16}
                    style={{
           width: '100%',
           padding: 14,
           borderRadius: 14,
           background: '#090810',
           border: '1px solid rgba(208, 188, 255, 0.25)',
           color: '#00e5ff',
           fontFamily: 'monospace',
           fontSize: 12.5,
           lineHeight: 1.5
          }}
         />
        </div>
       ) : (
        /* Visual Content Editor */
                <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
         {/* If Syllabus or Bundle: Render Subjects & Chapters Customizer */}
         {(editingPreset.category === 'Syllabus' || editingPreset.category === 'Bundle') && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 12, background: 'rgba(255, 255, 255, 0.02)', padding: 16, borderRadius: 16, border: '1px solid rgba(208, 188, 255, 0.15)' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
             <BookOpen size={16} color="#d0bcff" />
                          <h4 style={{ margin: 0, fontSize: 15, fontWeight: 800, color: '#fff' }}>
              Syllabus Curriculum ({editingPreset.data.subjects?.length || 0} Subjects)
             </h4>
            </div>

            <button
             onClick={() => {
              showCustomPrompt(
               'Add Subject to Preset',
               (val) => {
                if (!val.trim()) return;
                const newSub: Subject = {
                 id: `sub_${Date.now()}`,
                 name: val.trim(),
                 iconName: 'BookOpen',
                 papers: [
                  { id: `pap_1_${Date.now()}`, name: '1st Paper', chapters: [] },
                  { id: `pap_2_${Date.now()}`, name: '2nd Paper', chapters: [] }
                 ]
                };
                const subs = editingPreset.data.subjects ? [...editingPreset.data.subjects, newSub] : [newSub];
                setEditingPreset({ ...editingPreset, data: { ...editingPreset.data, subjects: subs } });
                showNotification(`Added subject "${val.trim()}"`);
               },
               { placeholder: 'e.g. ICT, General Knowledge' }
              );
             }}
                          style={{ padding: '6px 14px', borderRadius: 8, background: '#d0bcff', border: 'none', color: '#381e72', fontSize: 12, fontWeight: 800, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 5 }}
            >
             <Plus size={14} />
             <span>Add Subject</span>
            </button>
           </div>

           {/* Subjects & Chapters List */}
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {editingPreset.data.subjects?.map((sub, sIdx) => (
                          <div key={sub.id || sIdx} style={{ background: 'rgba(255, 255, 255, 0.04)', borderRadius: 12, padding: '12px 14px', border: '1px solid rgba(255, 255, 255, 0.08)' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
                              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                                <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.4)', fontWeight: 800 }}>#{sIdx + 1}</span>
                                <span style={{ fontSize: 15, fontWeight: 900, color: '#fff' }}>{sub.name}</span>
                                <span style={{ fontSize: 11, color: '#d0bcff', background: 'rgba(208, 188, 255, 0.15)', padding: '2px 6px', borderRadius: 4 }}>
                 {sub.papers.reduce((acc, p) => acc + p.chapters.length, 0)} Chapters
                </span>
               </div>

                              <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                <button
                 onClick={() => {
                  showCustomPrompt(
                   `Rename Subject "${sub.name}"`,
                   (val) => {
                    if (!val.trim()) return;
                    const subs = editingPreset.data.subjects!.map((s, idx) => idx === sIdx ? { ...s, name: val.trim() } : s);
                    setEditingPreset({ ...editingPreset, data: { ...editingPreset.data, subjects: subs } });
                   },
                   { initialValue: sub.name }
                  );
                 }}
                                  style={{ background: 'transparent', border: 'none', color: '#d0bcff', cursor: 'pointer', padding: 2 }}
                >
                 <Edit2 size={13} />
                </button>
                <button
                 onClick={() => {
                  const subs = editingPreset.data.subjects!.filter((_, idx) => idx !== sIdx);
                  setEditingPreset({ ...editingPreset, data: { ...editingPreset.data, subjects: subs } });
                 }}
                                  style={{ background: 'transparent', border: 'none', color: '#ef5350', cursor: 'pointer', padding: 2 }}
                >
                 <Trash2 size={13} />
                </button>
               </div>
              </div>

              {/* Papers & Chapters */}
                            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 260px), 1fr))', gap: 10 }}>
               {sub.papers.map((paper, pIdx) => (
                                <div key={paper.id || pIdx} style={{ background: 'rgba(0, 0, 0, 0.25)', borderRadius: 10, padding: 10, border: '1px solid rgba(255, 255, 255, 0.05)' }}>
                                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                                    <span style={{ fontSize: 12.5, fontWeight: 800, color: '#d0bcff' }}>{paper.name} ({paper.chapters.length})</span>
                  <button
                   onClick={() => {
                    showCustomPrompt(
                     `Add Chapter to ${sub.name} - ${paper.name}`,
                     (val) => {
                      if (!val.trim()) return;
                      const subs = [...editingPreset.data.subjects!];
                      const papers = [...subs[sIdx].papers];
                      papers[pIdx] = { ...papers[pIdx], chapters: [...papers[pIdx].chapters, { id: `ch_${Date.now()}`, name: val.trim(), sections: [] }] };
                      subs[sIdx] = { ...subs[sIdx], papers };
                      setEditingPreset({ ...editingPreset, data: { ...editingPreset.data, subjects: subs } });
                     }
                    );
                   }}
                                      style={{ background: 'rgba(208, 188, 255, 0.2)', border: 'none', color: '#d0bcff', padding: '2px 8px', borderRadius: 6, fontSize: 11, fontWeight: 800, cursor: 'pointer' }}
                  >
                   + Chapter
                  </button>
                 </div>

                                  <div style={{ display: 'flex', flexDirection: 'column', gap: 4, maxHeight: 150, overflowY: 'auto' }}>
                  {paper.chapters.map((chap, cIdx) => (
                                      <div key={chap.id || cIdx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 11.5, padding: '3px 6px', background: 'rgba(255, 255, 255, 0.03)', borderRadius: 6 }}>
                                        <span style={{ textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap', maxWidth: 180 }}>{cIdx + 1}. {chap.name}</span>
                                        <div style={{ display: 'flex', gap: 4 }}>
                     <button
                      onClick={() => {
                       showCustomPrompt(
                        'Rename Chapter',
                        (val) => {
                         if (!val.trim()) return;
                         const subs = [...editingPreset.data.subjects!];
                         const papers = [...subs[sIdx].papers];
                         const chaps = papers[pIdx].chapters.map((c, i) => i === cIdx ? { ...c, name: val.trim() } : c);
                         papers[pIdx] = { ...papers[pIdx], chapters: chaps };
                         subs[sIdx] = { ...subs[sIdx], papers };
                         setEditingPreset({ ...editingPreset, data: { ...editingPreset.data, subjects: subs } });
                        },
                        { initialValue: chap.name }
                       );
                      }}
                                            style={{ background: 'transparent', border: 'none', color: '#d0bcff', cursor: 'pointer', padding: 1 }}
                     >
                      <Edit2 size={10} />
                     </button>
                     <button
                      onClick={() => {
                       const subs = [...editingPreset.data.subjects!];
                       const papers = [...subs[sIdx].papers];
                       papers[pIdx] = { ...papers[pIdx], chapters: papers[pIdx].chapters.filter((_, i) => i !== cIdx) };
                       subs[sIdx] = { ...subs[sIdx], papers };
                       setEditingPreset({ ...editingPreset, data: { ...editingPreset.data, subjects: subs } });
                      }}
                                            style={{ background: 'transparent', border: 'none', color: '#ef5350', cursor: 'pointer', padding: 1 }}
                     >
                      <X size={11} />
                     </button>
                    </div>
                   </div>
                  ))}
                 </div>
                </div>
               ))}
              </div>
             </div>
            ))}
           </div>
          </div>
         )}

         {/* If Routine or Bundle: Render Routine Schedule Customizer */}
         {(editingPreset.category === 'Routine' || editingPreset.category === 'Bundle') && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 12, background: 'rgba(255, 255, 255, 0.02)', padding: 16, borderRadius: 16, border: '1px solid rgba(0, 229, 255, 0.15)' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
             <Calendar size={16} color="#00e5ff" />
                          <h4 style={{ margin: 0, fontSize: 15, fontWeight: 800, color: '#fff' }}>
              Routine Days Schedule ({editingPreset.data.routineDays?.length || 0} Days)
             </h4>
            </div>

                        <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
             <select
              value={editingPreset.data.routineMode || 'Offline'}
              onChange={(e) => setEditingPreset({ ...editingPreset, data: { ...editingPreset.data, routineMode: e.target.value as any } })}
                            style={{ padding: '5px 10px', borderRadius: 8, background: '#2b2930', border: '1px solid rgba(0, 229, 255, 0.3)', color: '#00e5ff', fontSize: 12, fontWeight: 800 }}
             >
              <option value="Offline">Offline Routine</option>
              <option value="Online">Online Routine</option>
             </select>

             <button
              onClick={() => {
               const newDay: RoutineItem = {
                id: `rot-${Date.now()}`,
                date: '01-Jan-26',
                day: 'Sunday',
                classSubject: 'Physics (P-01)',
                examDetails: 'Physics Weekly Exam',
                topics: ['Mechanics & Dynamics']
               };
               const days = editingPreset.data.routineDays ? [...editingPreset.data.routineDays, newDay] : [newDay];
               setEditingPreset({ ...editingPreset, data: { ...editingPreset.data, routineDays: days } });
              }}
                            style={{ padding: '6px 14px', borderRadius: 8, background: '#00e5ff', border: 'none', color: '#002633', fontSize: 12, fontWeight: 800, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 5 }}
             >
              <Plus size={14} />
              <span>Add Day</span>
             </button>
            </div>
           </div>

                      <div style={{ display: 'flex', flexDirection: 'column', gap: 6, maxHeight: 300, overflowY: 'auto' }}>
            {editingPreset.data.routineDays?.map((day, dIdx) => (
                          <div key={day.id || dIdx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'rgba(255,255,255,0.03)', padding: '8px 12px', borderRadius: 10, fontSize: 12.5 }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                              <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.4)', fontWeight: 800 }}>#{dIdx + 1}</span>
                              <span style={{ color: '#00e5ff', fontWeight: 800 }}>{day.date}</span>
                              <span style={{ color: '#fff', fontWeight: 700 }}>{day.classSubject || 'Self Study'}</span>
                              {day.examDetails && <span style={{ fontSize: 11, color: '#ffd54f' }}>({day.examDetails})</span>}
              </div>

              <button
               onClick={() => {
                const days = editingPreset.data.routineDays!.filter((_, i) => i !== dIdx);
                setEditingPreset({ ...editingPreset, data: { ...editingPreset.data, routineDays: days } });
               }}
                              style={{ background: 'transparent', border: 'none', color: '#ef5350', cursor: 'pointer', padding: 2 }}
              >
               <Trash2 size={13} />
              </button>
             </div>
            ))}
           </div>
          </div>
         )}

         {/* If Exams or Bundle: Render Exams Customizer */}
         {(editingPreset.category === 'Exams' || editingPreset.category === 'Bundle') && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 12, background: 'rgba(255, 255, 255, 0.02)', padding: 16, borderRadius: 16, border: '1px solid rgba(255, 213, 79, 0.15)' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
             <FileText size={16} color="#ffd54f" />
                          <h4 style={{ margin: 0, fontSize: 15, fontWeight: 800, color: '#fff' }}>
              Exam Series Schedule ({editingPreset.data.exams?.length || 0} Exams)
             </h4>
            </div>

            <button
             onClick={() => {
              const newExam = {
               date: '01-Jan-26',
               day: 'Sunday',
               exams: ['Physics Weekly Exam MCQ (15) + Written (10)'],
               syllabus: ['Mechanics & Vectors']
              };
              const exams = editingPreset.data.exams ? [...editingPreset.data.exams, newExam] : [newExam];
              setEditingPreset({ ...editingPreset, data: { ...editingPreset.data, exams } });
             }}
                          style={{ padding: '6px 14px', borderRadius: 8, background: '#ffd54f', border: 'none', color: '#3e2723', fontSize: 12, fontWeight: 800, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 5 }}
            >
             <Plus size={14} />
             <span>Add Exam</span>
            </button>
           </div>

                      <div style={{ display: 'flex', flexDirection: 'column', gap: 6, maxHeight: 300, overflowY: 'auto' }}>
            {editingPreset.data.exams?.map((ex, eIdx) => (
                          <div key={eIdx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'rgba(255,255,255,0.03)', padding: '8px 12px', borderRadius: 10, fontSize: 12.5 }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                              <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.4)', fontWeight: 800 }}>#{eIdx + 1}</span>
                              <span style={{ color: '#ffd54f', fontWeight: 800 }}>{ex.date}</span>
                              <span style={{ color: '#fff', fontWeight: 700 }}>{Array.isArray(ex.exams) ? ex.exams.join(', ') : ex.exams}</span>
              </div>

              <button
               onClick={() => {
                const exams = editingPreset.data.exams!.filter((_, i) => i !== eIdx);
                setEditingPreset({ ...editingPreset, data: { ...editingPreset.data, exams } });
               }}
                              style={{ background: 'transparent', border: 'none', color: '#ef5350', cursor: 'pointer', padding: 2 }}
              >
               <Trash2 size={13} />
              </button>
             </div>
            ))}
           </div>
          </div>
         )}
        </div>
       )}
      </div>
     ) : (
      /* Presets Studio List & Filter Grid */
      <>
       {/* Category Filter Pills & Search */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
                <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
         {(['All', 'Syllabus', 'Routine', 'Exams', 'Bundle'] as const).map((cat) => {
          const isSel = presetFilterCategory === cat;
          const count = cat === 'All' ? presetsList.length : presetsList.filter(p => p.category === cat).length;
          return (
           <button
            key={cat}
            onClick={() => setPresetFilterCategory(cat)}
                        style={{
             padding: '7px 16px',
             borderRadius: '12px',
             background: isSel ? '#d0bcff' : 'rgba(255, 255, 255, 0.06)',
             color: isSel ? '#381e72' : '#fff',
             border: 'none',
             fontWeight: 800,
             fontSize: 12.5,
             cursor: 'pointer',
             display: 'flex',
             alignItems: 'center',
             gap: 6
            }}
           >
            <span>{cat === 'All' ? 'All Tracks' : cat}</span>
                        <span style={{ fontSize: 11, opacity: 0.8 }}>({count})</span>
           </button>
          );
         })}
        </div>

                <div style={{ position: 'relative', width: 260 }}>
                  <Search size={15} style={{ position: 'absolute', left: 12, top: 11, color: 'rgba(255, 255, 255, 0.4)' }} />
         <input
          type="text"
          value={presetSearchQuery}
          onChange={(e) => setPresetSearchQuery(e.target.value)}
          placeholder="Search preset tracks..."
                    style={{
           width: '100%',
           padding: '8px 12px 8px 34px',
           borderRadius: '12px',
           background: 'rgba(255, 255, 255, 0.06)',
           border: '1px solid rgba(255, 255, 255, 0.12)',
           color: '#fff',
           fontSize: 12.5
          }}
         />
        </div>
       </div>

       {/* Presets Cards Grid */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(min(100%, 360px), 1fr))', gap: 16 }}>
        {presetsList
         .filter((p) => {
          const matchCat = presetFilterCategory === 'All' || p.category === presetFilterCategory;
          const q = presetSearchQuery.toLowerCase().trim();
          const matchQ = !q || p.name.toLowerCase().includes(q) || p.description.toLowerCase().includes(q) || p.category.toLowerCase().includes(q);
          return matchCat && matchQ;
         })
         .map((preset) => {
          const catBg = preset.category === 'Syllabus' ? 'rgba(208, 188, 255, 0.2)' : preset.category === 'Routine' ? 'rgba(0, 229, 255, 0.2)' : preset.category === 'Exams' ? 'rgba(255, 213, 79, 0.2)' : 'rgba(129, 199, 132, 0.2)';
          const catColor = preset.category === 'Syllabus' ? '#d0bcff' : preset.category === 'Routine' ? '#00e5ff' : preset.category === 'Exams' ? '#ffd54f' : '#81c784';

          return (
           <div
            key={preset.id}
            className="haze-card"
                        style={{
             borderRadius: '20px',
             padding: '20px',
             display: 'flex',
             flexDirection: 'column',
             gap: 12,
             border: '1px solid rgba(255, 255, 255, 0.1)',
             background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.03) 0%, rgba(208, 188, 255, 0.03) 100%)'
            }}
           >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 10 }}>
                          <div style={{ display: 'flex', flexDirection: 'column', gap: 4, flex: 1 }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                              <span style={{ fontSize: 11, fontWeight: 900, padding: '2px 8px', borderRadius: 6, background: catBg, color: catColor }}>
                {preset.category}
               </span>
                              <span style={{ fontSize: 11, color: 'rgba(255, 255, 255, 0.4)' }}>
                {new Date(preset.createdAt).toLocaleDateString()}
               </span>
              </div>
                            <h4 style={{ margin: 0, fontSize: 16, fontWeight: 900, color: '#fff' }}>
               {preset.name}
              </h4>
             </div>

                          <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              {/* Duplicate */}
              <button
               onClick={() => {
                const dup: PresetTemplate = {
                 ...JSON.parse(JSON.stringify(preset)),
                 id: `preset_${Date.now()}`,
                 name: `${preset.name} (Copy)`,
                 createdAt: Date.now()
                };
                setPresetsList((prev) => {
                 const updated = [dup, ...prev];
                 localStorage.setItem('eap_admin_presets', JSON.stringify(updated));
                 return updated;
                });
                sound.playSuccess();
                showNotification(`Duplicated preset "${dup.name}"`);
               }}
               title="Duplicate Track"
                              style={{ padding: '6px 8px', borderRadius: 8, background: 'rgba(255, 255, 255, 0.06)', border: '1px solid rgba(255, 255, 255, 0.12)', color: 'rgba(255, 255, 255, 0.8)', cursor: 'pointer' }}
              >
               <Copy size={13} />
              </button>

              {/* Export JSON */}
              <button
               onClick={() => {
                const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(preset, null, 2));
                const downloadAnchor = document.createElement('a');
                downloadAnchor.setAttribute('href', dataStr);
                downloadAnchor.setAttribute('download', `${preset.name.replace(/\s+/g, '_').toLowerCase()}.json`);
                downloadAnchor.click();
               }}
               title="Download Preset JSON"
                              style={{ padding: '6px 8px', borderRadius: 8, background: 'rgba(255, 255, 255, 0.06)', border: '1px solid rgba(255, 255, 255, 0.12)', color: 'rgba(255, 255, 255, 0.8)', cursor: 'pointer' }}
              >
               <Download size={13} />
              </button>

              {/* Delete */}
              <button
               onClick={() => {
                showCustomConfirm(
                 `Delete Preset "${preset.name}"?`,
                 `Are you sure you want to delete this preset from the library?`,
                 () => {
                  setPresetsList((prev) => {
                   const updated = prev.filter(p => p.id !== preset.id);
                   localStorage.setItem('eap_admin_presets', JSON.stringify(updated));
                   return updated;
                  });
                  showNotification('Preset deleted');
                 },
                 { confirmText: 'Delete Preset', cancelText: 'Cancel', isDanger: true }
                );
               }}
               title="Delete Preset"
                              style={{ padding: '6px 8px', borderRadius: 8, background: 'rgba(239, 83, 80, 0.15)', border: '1px solid rgba(239, 83, 80, 0.3)', color: '#ef5350', cursor: 'pointer' }}
              >
               <Trash2 size={13} />
              </button>
             </div>
            </div>

                        <p style={{ margin: 0, fontSize: 12.5, color: 'rgba(255, 255, 255, 0.7)', lineHeight: 1.45 }}>
             {preset.description}
            </p>

            {/* Content summary badges */}
                        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
             {preset.data.subjects && (
                            <span style={{ fontSize: 11, color: '#d0bcff', background: 'rgba(208, 188, 255, 0.1)', padding: '3px 8px', borderRadius: 6, fontWeight: 700 }}>
                {preset.data.subjects.length} Subjects ({preset.data.subjects.map(s => s.name).join(', ')})
              </span>
             )}
             {preset.data.routineDays && (
                            <span style={{ fontSize: 11, color: '#00e5ff', background: 'rgba(0, 229, 255, 0.1)', padding: '3px 8px', borderRadius: 6, fontWeight: 700 }}>
                {preset.data.routineDays.length} Days ({preset.data.routineMode || 'Offline'})
              </span>
             )}
             {preset.data.exams && (
                            <span style={{ fontSize: 11, color: '#ffd54f', background: 'rgba(255, 213, 79, 0.1)', padding: '3px 8px', borderRadius: 6, fontWeight: 700 }}>
                {preset.data.exams.length} Exams
              </span>
             )}
            </div>

            {/* Bottom Action Bar */}
                        <div style={{ display: 'flex', gap: 8, marginTop: 4, paddingTop: 10, borderTop: '1px solid rgba(255, 255, 255, 0.06)' }}>
             <button
              onClick={() => {
               setSelectedPresetForMassAssign(preset);
               setIsMassAssignModalOpen(true);
              }}
                            style={{
               flex: 1,
               padding: '8px 12px',
               borderRadius: '10px',
               background: 'linear-gradient(135deg, #d0bcff, #b69df8)',
               border: 'none',
               color: '#381e72',
               fontSize: 12,
               fontWeight: 900,
               cursor: 'pointer',
               display: 'flex',
               alignItems: 'center',
               justifyContent: 'center',
               gap: 6
              }}
             >
              <Send size={13} />
              <span>Deploy to Students </span>
             </button>

             <button
              onClick={() => {
               setEditingPreset(JSON.parse(JSON.stringify(preset)));
               setIsCreatingNewPreset(false);
               setPresetEditorTab('visual');
               setPresetRawJsonText(JSON.stringify(preset.data, null, 2));
              }}
                            style={{
               padding: '8px 14px',
               borderRadius: '10px',
               background: 'rgba(255, 255, 255, 0.08)',
               border: '1px solid rgba(255, 255, 255, 0.15)',
               color: '#fff',
               fontSize: 12,
               fontWeight: 800,
               cursor: 'pointer',
               display: 'flex',
               alignItems: 'center',
               gap: 5
              }}
             >
              <Edit3 size={13} />
              <span>Edit & Customize</span>
             </button>
            </div>
           </div>
          );
         })}
       </div>
      </>
     )}
    </div>
   )}

   {/* TAB 4: UNIVERSAL REST API & GOOGLE DRIVE CLOUD HUB */}
   {activeTab === 'cloud' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
     {/* 0. Google Drive Cloud Backup & Restore Hub Card */}
     <div
      className="haze-card"
      style={{
       borderRadius: '26px',
       padding: '26px',
       display: 'flex',
       flexDirection: 'column',
       gap: 18,
       border: '1px solid rgba(66, 133, 244, 0.35)',
       background: 'linear-gradient(135deg, rgba(66, 133, 244, 0.08), rgba(52, 168, 83, 0.05))',
       boxShadow: '0 8px 32px rgba(0,0,0,0.3)'
      }}
     >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
       <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
        <div
         style={{
          width: 48,
          height: 48,
          borderRadius: '16px',
          background: 'rgba(66, 133, 244, 0.2)',
          border: '1px solid rgba(66, 133, 244, 0.4)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
         }}
        >
         <svg width="26" height="26" viewBox="0 0 87.3 78">
          <path d="m6.6 66.85 3.85 6.65c.8 1.4 1.9 2.5 3.2 3.3l12.8-22.2h-26.4c0 1.6.4 3.1 1.2 4.5z" fill="#0066da"/>
          <path d="m43.65 25-12.8-22.2c-1.3.8-2.4 1.9-3.2 3.3l-26.4 45.7c-.8 1.4-1.2 2.9-1.2 4.5h26.4z" fill="#00ac47"/>
          <path d="m73.55 76.8c1.3-.8 2.4-1.9 3.2-3.3l1.6-2.75 7.6-13.15c.8-1.4 1.2-2.9 1.2-4.5h-26.4l6.4 11.1z" fill="#ea4335"/>
          <path d="m43.65 25 12.8-22.2c-1.3-.8-2.9-1.2-4.5-1.2h-16.5c-1.6 0-3.1.4-4.5 1.2z" fill="#00832d"/>
          <path d="m59.8 54.6h-32.5l-13.65 23.4c1.4.8 2.9 1.2 4.5 1.2h50.7c1.6 0 3.1-.4 4.5-1.2z" fill="#2684fc"/>
          <path d="m73.4 26.5-12.8-22.2c-.8-1.4-1.9-2.5-3.2-3.3l-13.75 24h26.4c1.6 0 3.1.4 4.5 1.2.8.4 1.4.9 2 1.5z" fill="#ffba00"/>
         </svg>
        </div>
        <div>
         <h3 style={{ margin: 0, fontSize: 18, fontWeight: 900, color: '#fff' }}>
          Google Drive Cloud Backup Hub
         </h3>
         <p style={{ margin: '3px 0 0', fontSize: 12.5, color: 'rgba(255, 255, 255, 0.7)' }}>
          Directly sync & back up your master admission routines, exams, syllabus & settings to your personal Google Drive.
         </p>
        </div>
       </div>

       <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        {googleAdminUser ? (
         <div
          style={{
           display: 'flex',
           alignItems: 'center',
           gap: 8,
           padding: '6px 12px',
           borderRadius: '12px',
           background: 'rgba(52, 168, 83, 0.15)',
           border: '1px solid rgba(52, 168, 83, 0.35)',
           fontSize: 12,
           color: '#81c784',
           fontWeight: 700
          }}
         >
          <Check size={14} />
          <span>Connected: {googleAdminUser.email}</span>
         </div>
        ) : (
         <button
          onClick={handleGoogleSignIn}
          style={{
           padding: '8px 14px',
           borderRadius: '12px',
           background: '#4285F4',
           color: '#fff',
           border: 'none',
           fontWeight: 800,
           fontSize: 12.5,
           cursor: 'pointer',
           display: 'flex',
           alignItems: 'center',
           gap: 6
          }}
         >
          <Key size={14} />
          <span>Connect Google Drive</span>
         </button>
        )}
       </div>
      </div>

      {/* Metadata Status Chips */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 10 }}>
       <div style={{ padding: '12px 14px', borderRadius: '14px', background: 'rgba(255, 255, 255, 0.05)', border: '1px solid rgba(255, 255, 255, 0.1)' }}>
        <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.6)', fontWeight: 600 }}>Last Cloud Backup</span>
        <div style={{ fontSize: 14, fontWeight: 800, color: '#fff', marginTop: 4 }}>{gdriveLastTime}</div>
       </div>
       <div style={{ padding: '12px 14px', borderRadius: '14px', background: 'rgba(255, 255, 255, 0.05)', border: '1px solid rgba(255, 255, 255, 0.1)' }}>
        <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.6)', fontWeight: 600 }}>Backup Bundle Size</span>
        <div style={{ fontSize: 14, fontWeight: 800, color: '#81c784', marginTop: 4 }}>{gdriveLastSize}</div>
       </div>
       <div style={{ padding: '12px 14px', borderRadius: '14px', background: 'rgba(255, 255, 255, 0.05)', border: '1px solid rgba(255, 255, 255, 0.1)' }}>
        <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.6)', fontWeight: 600 }}>Cloud Storage Provider</span>
        <div style={{ fontSize: 14, fontWeight: 800, color: '#4285F4', marginTop: 4 }}>Google Drive (Drive API v3)</div>
       </div>
      </div>

      {/* Quick Action Buttons */}
      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
       <button
        onClick={handlePerformGoogleDriveBackup}
        disabled={isGdriveBackingUp}
        style={{
         padding: '12px 22px',
         borderRadius: '14px',
         background: 'linear-gradient(135deg, #4285F4, #34A853)',
         color: '#fff',
         border: 'none',
         fontWeight: 900,
         fontSize: 14,
         cursor: isGdriveBackingUp ? 'not-allowed' : 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 8,
         boxShadow: '0 6px 20px rgba(66, 133, 244, 0.35)'
        }}
       >
        <CloudUpload size={18} />
        <span>{isGdriveBackingUp ? 'Uploading to Drive...' : 'Back Up to Google Drive Now'}</span>
       </button>

       <button
        onClick={() => {
         loadGdriveBackups();
         setIsRestoreModalOpen(true);
        }}
        style={{
         padding: '12px 20px',
         borderRadius: '14px',
         background: 'rgba(255, 255, 255, 0.08)',
         border: '1px solid rgba(255, 255, 255, 0.2)',
         color: '#fff',
         fontWeight: 800,
         fontSize: 13.5,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 8
        }}
       >
        <CloudDownload size={17} />
        <span>Restore from Google Drive</span>
       </button>

       <button
        onClick={handleExportFullMasterJSON}
        title="Download JSON to computer storage"
        style={{
         padding: '12px 18px',
         borderRadius: '14px',
         background: 'rgba(255, 255, 255, 0.04)',
         border: '1px solid rgba(255, 255, 255, 0.15)',
         color: 'rgba(255, 255, 255, 0.85)',
         fontWeight: 700,
         fontSize: 13,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <Download size={15} />
        <span>Offline JSON Backup</span>
       </button>
      </div>
     </div>

     {/* 1. Standalone Direct Instant Push Broadcast Card */}
     <div
      className="haze-card"
            style={{
       borderRadius: '24px',
       padding: '24px',
       display: 'flex',
       flexDirection: 'column',
       gap: 16,
       border: '1px solid rgba(208, 188, 255, 0.25)'
      }}
     >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <Bell size={22} color="#d0bcff" />
                <h3 style={{ margin: 0, fontSize: 17, fontWeight: 900, color: '#fff' }}>
         Standalone Direct Instant Push Notification Sender
        </h3>
       </div>
              <span style={{
        fontSize: 11,
        fontWeight: 800,
        color: '#81c784',
        background: 'rgba(129, 199, 132, 0.15)',
        padding: '4px 10px',
        borderRadius: '12px'
       }}>
        Instant Push Mode
       </span>
      </div>

            <p style={{ margin: 0, fontSize: 13, color: 'rgba(255, 255, 255, 0.75)', lineHeight: '18px' }}>
       Send a direct alert or announcement to all students' phone notification bars immediately, without needing to sync or modify the class routines.
      </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 220px), 1fr))', gap: 10 }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <label style={{ fontSize: 12, fontWeight: 700, color: '#d0bcff' }}>
          Notification Title
         </label>
         <input
          type="text"
          placeholder="e.g. Urgent Notice: Class Shifted"
          value={directNotifTitle}
          onChange={(e) => setDirectNotifTitle(e.target.value)}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: 'rgba(255, 255, 255, 0.06)',
           border: '1px solid rgba(208, 188, 255, 0.25)',
           color: '#fff',
           fontSize: 13.5
          }}
         />
        </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <label style={{ fontSize: 12, fontWeight: 700, color: '#d0bcff' }}>
          Category Badge
         </label>
         <select
          value={directNotifCategory}
          onChange={(e) => setDirectNotifCategory(e.target.value as any)}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: '#2b2930',
           border: '1px solid rgba(208, 188, 255, 0.25)',
           color: '#fff',
           fontSize: 13.5
          }}
         >
          <option value="Notice">General Notice</option>
          <option value="Alert">Urgent Alert</option>
          <option value="Motivation">Daily Motivation</option>
          <option value="Exam">Exam Schedule Alert</option>
         </select>
        </div>
       </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <label style={{ fontSize: 12, fontWeight: 700, color: '#d0bcff' }}>
         Notification Message Body
        </label>
        <textarea
         rows={2}
         placeholder="Type your notification message here..."
         value={directNotifBody}
         onChange={(e) => setDirectNotifBody(e.target.value)}
                  style={{
          padding: '12px 14px',
          borderRadius: '12px',
          background: 'rgba(255, 255, 255, 0.06)',
          border: '1px solid rgba(208, 188, 255, 0.25)',
          color: '#fff',
          fontSize: 13.5,
          resize: 'vertical',
          fontFamily: 'inherit'
         }}
        />
       </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 200px), 1fr))', gap: 10 }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <label style={{ fontSize: 12, fontWeight: 700, color: '#d0bcff' }}>
          External Web Link (Optional)
         </label>
         <input
          type="url"
          placeholder="https://example.com/live-class"
          value={directNotifUrl}
          onChange={(e) => setDirectNotifUrl(e.target.value)}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: 'rgba(255, 255, 255, 0.06)',
           border: '1px solid rgba(208, 188, 255, 0.25)',
           color: '#fff',
           fontSize: 13.5
          }}
         />
        </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <label style={{ fontSize: 12, fontWeight: 700, color: '#d0bcff' }}>
          Action Button Label (Optional)
         </label>
         <input
          type="text"
          placeholder="e.g. Join Class / Visit Link"
          value={directNotifButtonText}
          onChange={(e) => setDirectNotifButtonText(e.target.value)}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: 'rgba(255, 255, 255, 0.06)',
           border: '1px solid rgba(208, 188, 255, 0.25)',
           color: '#fff',
           fontSize: 13.5
          }}
         />
        </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <label style={{ fontSize: 12, fontWeight: 700, color: '#d0bcff' }}>
          App Deep Link Navigation
         </label>
         <select
          value={directNotifRoute}
          onChange={(e) => setDirectNotifRoute(e.target.value)}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: '#2b2930',
           border: '1px solid rgba(208, 188, 255, 0.25)',
           color: '#fff',
           fontSize: 13.5
          }}
         >
          <option value="">None (Notification inbox only)</option>
          <option value="syllabus">Open Syllabus Screen</option>
          <option value="exams">Open Exams & Marks Screen</option>
          <option value="routine">Open Class Routine Screen</option>
          <option value="progress">Open Progress Graph Screen</option>
          <option value="profile">Open Profile & Settings</option>
         </select>
        </div>
       </div>

       <button
        onClick={() => handleSendDirectNotification()}
        disabled={!directNotifBody.trim() || isSyncing}
                style={{
         padding: '14px 20px',
         borderRadius: '14px',
         background: directNotifBody.trim() ? '#d0bcff' : 'rgba(208, 188, 255, 0.3)',
         color: '#381e72',
         border: 'none',
         fontWeight: 900,
         fontSize: 14,
         cursor: directNotifBody.trim() ? 'pointer' : 'not-allowed',
         display: 'flex',
         alignItems: 'center',
         justifyContent: 'center',
         gap: 8,
         transition: 'all 0.2s ease'
        }}
       >
        <Send size={18} />
        <span>Send Instant Push Notification to Students Now</span>
       </button>
      </div>
     </div>

     {/* 2. Official Target Exam & Countdown Manager Card */}
     <div
      className="haze-card"
            style={{
       borderRadius: '24px',
       padding: '24px',
       display: 'flex',
       flexDirection: 'column',
       gap: 16
      }}
     >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <Target size={22} color="#ffd54f" />
                <h3 style={{ margin: 0, fontSize: 17, fontWeight: 900, color: '#fff' }}>
         Official Target Exam & Countdown Manager
        </h3>
       </div>
              <span style={{
        fontSize: 11,
        fontWeight: 800,
        color: '#ffd54f',
        background: 'rgba(255, 213, 79, 0.15)',
        padding: '4px 10px',
        borderRadius: '12px'
       }}>
        Target Timer
       </span>
      </div>

            <p style={{ margin: 0, fontSize: 13, color: 'rgba(255, 255, 255, 0.75)', lineHeight: '18px' }}>
       Set the official target exam date or countdown. Updates all students' home and profile countdowns in real time.
      </p>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 220px), 1fr))', gap: 12 }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <label style={{ fontSize: 12, fontWeight: 700, color: '#d0bcff' }}>
         Target Exam Preset
        </label>
        <select
         value={targetExamPreset}
         onChange={(e) => handleTargetExamPresetChange(e.target.value)}
                  style={{
          padding: '12px 14px',
          borderRadius: '12px',
          background: '#2b2930',
          border: '1px solid rgba(208, 188, 255, 0.25)',
          color: '#fff',
          fontSize: 13.5
         }}
        >
         {admissionExamPresets.map((preset, pIdx) => (
          <option key={pIdx} value={preset.name}>
           {preset.name} ({preset.days} Days)
          </option>
         ))}
         <option value="Custom">Custom Target Exam Name</option>
        </select>
       </div>

       {targetExamPreset === 'Custom' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <label style={{ fontSize: 12, fontWeight: 700, color: '#d0bcff' }}>
          Custom Exam Name
         </label>
         <input
          type="text"
          placeholder="e.g. MIST Admission Test 2026"
          value={targetExamCustomName}
          onChange={(e) => setTargetExamCustomName(e.target.value)}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: 'rgba(255, 255, 255, 0.06)',
           border: '1px solid rgba(208, 188, 255, 0.25)',
           color: '#fff',
           fontSize: 13.5
          }}
         />
        </div>
       )}

       {/* Calendar Date Picker */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <label style={{ fontSize: 12, fontWeight: 700, color: '#ffd54f', display: 'flex', alignItems: 'center', gap: 4 }}>
         <Calendar size={13} />
         <span>Exact Exam Date (Calendar Picker)</span>
        </label>
        <input
         type="date"
         value={targetExamDateInput}
         onChange={(e) => handleTargetExamDateChange(e.target.value)}
                  style={{
          padding: '12px 14px',
          borderRadius: '12px',
          background: 'rgba(255, 255, 255, 0.08)',
          border: '1px solid rgba(255, 213, 79, 0.35)',
          color: '#ffd54f',
          fontSize: 13.5,
          fontWeight: 700
         }}
        />
       </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <label style={{ fontSize: 12, fontWeight: 700, color: '#d0bcff' }}>
         Days Remaining
        </label>
        <input
         type="number"
         min={1}
         max={365}
         value={targetExamDays}
         onChange={(e) => {
          const days = Math.max(1, parseInt(e.target.value) || 1);
          setTargetExamDays(days);
          setTargetExamDateInput(calculateDefaultExamDate(days));
         }}
                  style={{
          padding: '12px 14px',
          borderRadius: '12px',
          background: 'rgba(255, 255, 255, 0.06)',
          border: '1px solid rgba(208, 188, 255, 0.25)',
          color: '#fff',
          fontSize: 13.5
         }}
        />
       </div>
      </div>

      {/* Live Calculated Target Date & Status Banner */}
            <div style={{
       display: 'flex',
       alignItems: 'center',
       justifyContent: 'space-between',
       flexWrap: 'wrap',
       gap: 8,
       padding: '12px 16px',
       borderRadius: '14px',
       background: 'rgba(255, 213, 79, 0.08)',
       border: '1px solid rgba(255, 213, 79, 0.25)'
      }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#ffd54f', fontSize: 13, fontWeight: 800 }}>
        <Calendar size={16} />
        <span>Target Date: {new Date(Date.now() + targetExamDays * 86400000).toLocaleDateString('en-US', { weekday: 'short', year: 'numeric', month: 'long', day: 'numeric' })}</span>
       </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'rgba(255, 255, 255, 0.8)', fontSize: 12.5, fontWeight: 700 }}>
        <Clock size={15} color="#ffd54f" />
        <span>Exact Countdown: {targetExamDays} Days ({Math.round(targetExamDays / 30 * 10) / 10} Months)</span>
       </div>
      </div>

            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
       <button
        onClick={handleBroadcastTargetExam}
        disabled={isSyncing}
                style={{
         flex: 1,
         minWidth: 260,
         padding: '14px 20px',
         borderRadius: '14px',
         background: 'rgba(255, 213, 79, 0.2)',
         border: '1px solid rgba(255, 213, 79, 0.4)',
         color: '#ffd54f',
         fontWeight: 900,
         fontSize: 14,
         cursor: isSyncing ? 'default' : 'pointer',
         display: 'flex',
         alignItems: 'center',
         justifyContent: 'center',
         gap: 8,
         transition: 'all 0.2s ease'
        }}
       >
        <Target size={18} />
        <span>Broadcast Official Exam Countdown to All Students</span>
       </button>

       <button
        onClick={handleClearTargetExam}
        disabled={isSyncing}
        title="Reset target countdown on student apps"
                style={{
         padding: '14px 20px',
         borderRadius: '14px',
         background: 'rgba(239, 83, 80, 0.15)',
         border: '1px solid rgba(239, 83, 80, 0.35)',
         color: '#ef5350',
         fontWeight: 800,
         fontSize: 13.5,
         cursor: isSyncing ? 'default' : 'pointer',
         display: 'flex',
         alignItems: 'center',
         justifyContent: 'center',
         gap: 6
        }}
       >
        <RotateCcw size={16} />
        <span>Clear / Reset Countdown</span>
       </button>
      </div>
     </div>

     {/* 3. API Configuration Card */}
     <div
      className="haze-card"
            style={{
       borderRadius: '24px',
       padding: '24px',
       display: 'flex',
       flexDirection: 'column',
       gap: 16
      }}
     >
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
       <Key size={22} color="#d0bcff" />
              <h3 style={{ margin: 0, fontSize: 17, fontWeight: 900, color: '#fff' }}>
        Universal REST API Configuration
       </h3>
      </div>

            <p style={{ margin: 0, fontSize: 13, color: 'rgba(255, 255, 255, 0.75)', lineHeight: '18px' }}>
       Connect your Web Admin to any free cloud endpoint (e.g. Firebase Realtime REST, JSONBin, Cloudflare Workers, Vercel/Render API). Clicking <strong>Publish</strong> will broadcast all schedule updates directly to every student's Android and Web app!
      </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <label style={{ fontSize: 12, fontWeight: 700, color: '#d0bcff' }}>
         REST API Endpoint URL
        </label>
        <input
         type="text"
         placeholder="https://your-api.com/api/eap-schedule or https://api.jsonbin.io/v3/b/..."
         value={apiEndpoint}
         onChange={(e) => setApiEndpoint(e.target.value)}
                  style={{
          padding: '12px 14px',
          borderRadius: '12px',
          background: 'rgba(255, 255, 255, 0.06)',
          border: '1px solid rgba(208, 188, 255, 0.25)',
          color: '#fff',
          fontSize: 14
         }}
        />
       </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <label style={{ fontSize: 12, fontWeight: 700, color: '#d0bcff' }}>
         Secret API Key / Bearer Token
        </label>
        <input
         type="password"
         placeholder="Enter your secret admin API Key (e.g. $2a$10$...)"
         value={apiKey}
         onChange={(e) => {
          setApiKey(e.target.value);
          localStorage.setItem('eap_rest_api_key', e.target.value);
         }}
                  style={{
          padding: '12px 14px',
          borderRadius: '12px',
          background: 'rgba(255, 255, 255, 0.06)',
          border: '1px solid rgba(208, 188, 255, 0.25)',
          color: '#fff',
          fontSize: 14
         }}
        />
       </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <label style={{ fontSize: 12, fontWeight: 700, color: '#81c784', display: 'flex', alignItems: 'center', gap: 6 }}>
          <Zap size={14} color="#81c784" />
          <span>Cloudflare Worker Push Endpoint (FCM HTTP v1)</span>
         </label>
                  <span style={{ fontSize: 11, color: 'rgba(129, 199, 132, 0.8)' }}>
          (Modern, secure FCM push notification sender)
         </span>
        </div>
        <input
         type="url"
         placeholder="https://eap-fcm-worker.yourdomain.workers.dev"
         value={workerUrl}
         onChange={(e) => {
          setWorkerUrl(e.target.value);
          localStorage.setItem('eap_cf_worker_url', e.target.value);
         }}
                  style={{
          padding: '12px 14px',
          borderRadius: '12px',
          background: 'rgba(129, 199, 132, 0.08)',
          border: '1px solid rgba(129, 199, 132, 0.35)',
          color: '#fff',
          fontSize: 14
         }}
        />
       </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <label style={{ fontSize: 12, fontWeight: 700, color: '#81c784', display: 'flex', alignItems: 'center', gap: 6 }}>
          <Zap size={14} color="#81c784" />
          <span>Worker Authorization Secret</span>
         </label>
                  <span style={{ fontSize: 11, color: 'rgba(129, 199, 132, 0.8)' }}>
          (Must match ADMIN_SECRET inside Worker)
         </span>
        </div>
        <input
         type="password"
         placeholder="Enter the secure admin token for the worker"
         value={workerSecret}
         onChange={(e) => {
          setWorkerSecret(e.target.value);
          localStorage.setItem('eap_cf_worker_secret', e.target.value);
         }}
                  style={{
          padding: '12px 14px',
          borderRadius: '12px',
          background: 'rgba(129, 199, 132, 0.08)',
          border: '1px solid rgba(129, 199, 132, 0.35)',
          color: '#fff',
          fontSize: 14
         }}
        />
       </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <label style={{ fontSize: 12, fontWeight: 700, color: '#d0bcff', display: 'flex', alignItems: 'center', gap: 6 }}>
          <Bell size={14} color="#d0bcff" />
          <span>Announcement / Notification Message for Students</span>
         </label>
                  <span style={{ fontSize: 11, color: 'rgba(255, 255, 255, 0.5)' }}>
          (Appears directly on students' phone notifications)
         </span>
        </div>
        <textarea
         rows={2}
         placeholder="e.g. Physics P-02 class and exam rescheduled to Saturday 10 AM! Check latest routine."
         value={updateMessage}
         onChange={(e) => {
          setUpdateMessage(e.target.value);
          localStorage.setItem('eap_update_msg', e.target.value);
         }}
                  style={{
          padding: '12px 14px',
          borderRadius: '12px',
          background: 'rgba(255, 255, 255, 0.06)',
          border: '1px solid rgba(208, 188, 255, 0.25)',
          color: '#fff',
          fontSize: 13.5,
          resize: 'vertical',
          fontFamily: 'inherit'
         }}
        />
       </div>
      </div>

      {/* Granular Live Cloud Publishing Grid */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 14, marginTop: 8 }}>
       {/* Row 1: Full Master Bundle & Fetch */}
              <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
        <button
         onClick={() => handlePublishModule('master')}
         disabled={isSyncing}
                  style={{
          flex: 1,
          minWidth: 220,
          padding: '14px 20px',
          borderRadius: '14px',
          background: 'linear-gradient(135deg, #d0bcff, #b69df8)',
          color: '#381e72',
          border: 'none',
          fontSize: 14,
          fontWeight: 900,
          cursor: isSyncing ? 'default' : 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 8,
          boxShadow: '0 4px 16px rgba(208, 188, 255, 0.3)'
         }}
        >
         {isSyncing ? <RefreshCw size={18} className="spin" /> : <CloudUpload size={18} />}
         {isSyncing ? 'Broadcasting...' : 'Publish Full Master Bundle (Everything)'}
        </button>

        <button
         onClick={handleFetchFromCloud}
         disabled={isSyncing}
                  style={{
          padding: '14px 20px',
          borderRadius: '14px',
          background: 'rgba(255, 255, 255, 0.08)',
          border: '1px solid rgba(255, 255, 255, 0.2)',
          color: '#fff',
          fontSize: 14,
          fontWeight: 800,
          cursor: isSyncing ? 'default' : 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: 8
         }}
        >
         <CloudDownload size={18} />
         Fetch Cloud Data
        </button>
       </div>

       {/* Row 2: Selective Routine Live Push */}
              <div style={{ background: 'rgba(0, 229, 255, 0.05)', padding: 14, borderRadius: 14, border: '1px solid rgba(0, 229, 255, 0.2)' }}>
                <div style={{ fontSize: 12, fontWeight: 800, color: '#00e5ff', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 6 }}>
         <BookOpen size={14} /> Selective Class Routine Live Sync (Updates only routines without touching exams)
        </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))', gap: 8 }}>
         <button
          onClick={() => handlePublishModule('offlineRoutine')}
          disabled={isSyncing}
                    style={{
           padding: '10px 14px',
           borderRadius: '10px',
           background: 'rgba(0, 229, 255, 0.15)',
           border: '1px solid rgba(0, 229, 255, 0.35)',
           color: '#00e5ff',
           fontWeight: 800,
           fontSize: 12,
           cursor: isSyncing ? 'default' : 'pointer',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           gap: 6
          }}
         >
          <Zap size={14} />
          Push Offline Routine Only
         </button>

         <button
          onClick={() => handlePublishModule('onlineRoutine')}
          disabled={isSyncing}
                    style={{
           padding: '10px 14px',
           borderRadius: '10px',
           background: 'rgba(0, 229, 255, 0.15)',
           border: '1px solid rgba(0, 229, 255, 0.35)',
           color: '#00e5ff',
           fontWeight: 800,
           fontSize: 12,
           cursor: isSyncing ? 'default' : 'pointer',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           gap: 6
          }}
         >
          <Zap size={14} />
          Push Online Routine Only
         </button>

         <button
          onClick={() => handlePublishModule('allRoutines')}
          disabled={isSyncing}
                    style={{
           padding: '10px 14px',
           borderRadius: '10px',
           background: 'rgba(0, 229, 255, 0.22)',
           border: '1px solid rgba(0, 229, 255, 0.45)',
           color: '#00e5ff',
           fontWeight: 800,
           fontSize: 12,
           cursor: isSyncing ? 'default' : 'pointer',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           gap: 6
          }}
         >
          <Layers size={14} />
          Push Both Routines Live
         </button>
        </div>
       </div>

       {/* Row 3: Selective Exam Schedule Live Push */}
              <div style={{ background: 'rgba(255, 183, 77, 0.05)', padding: 14, borderRadius: 14, border: '1px solid rgba(255, 183, 77, 0.2)' }}>
                <div style={{ fontSize: 12, fontWeight: 800, color: '#ffb74d', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 6 }}>
         <Calendar size={14} /> Selective Exam Schedule Live Sync (Updates only exams without touching class routines)
        </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))', gap: 8 }}>
         <button
          onClick={() => handlePublishModule('offlineExams')}
          disabled={isSyncing}
                    style={{
           padding: '10px 14px',
           borderRadius: '10px',
           background: 'rgba(255, 183, 77, 0.15)',
           border: '1px solid rgba(255, 183, 77, 0.35)',
           color: '#ffb74d',
           fontWeight: 800,
           fontSize: 12,
           cursor: isSyncing ? 'default' : 'pointer',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           gap: 6
          }}
         >
          <Zap size={14} />
          Push Offline Exams Only
         </button>

         <button
          onClick={() => handlePublishModule('onlineExams')}
          disabled={isSyncing}
                    style={{
           padding: '10px 14px',
           borderRadius: '10px',
           background: 'rgba(255, 183, 77, 0.15)',
           border: '1px solid rgba(255, 183, 77, 0.35)',
           color: '#ffb74d',
           fontWeight: 800,
           fontSize: 12,
           cursor: isSyncing ? 'default' : 'pointer',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           gap: 6
          }}
         >
          <Zap size={14} />
          Push Online Exams Only
         </button>

         <button
          onClick={() => handlePublishModule('allExams')}
          disabled={isSyncing}
                    style={{
           padding: '10px 14px',
           borderRadius: '10px',
           background: 'rgba(255, 183, 77, 0.22)',
           border: '1px solid rgba(255, 183, 77, 0.45)',
           color: '#ffb74d',
           fontWeight: 800,
           fontSize: 12,
           cursor: isSyncing ? 'default' : 'pointer',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           gap: 6
          }}
         >
          <Layers size={14} />
          Push Both Exams Live
         </button>
        </div>
       </div>
      </div>
     </div>

     {/* Master JSON Backup & Restore Card */}
     <div
      className="haze-card"
            style={{
       borderRadius: '24px',
       padding: '24px',
       display: 'flex',
       flexDirection: 'column',
       gap: 16
      }}
     >
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
       <Database size={22} color="#d0bcff" />
              <h3 style={{ margin: 0, fontSize: 17, fontWeight: 900, color: '#fff' }}>
        Offline JSON Backup & Restore System
       </h3>
      </div>

            <p style={{ margin: 0, fontSize: 13, color: 'rgba(255, 255, 255, 0.75)', lineHeight: '18px' }}>
       Save, export, or import your entire EAP routine and exam dataset as an offline <code>.json</code> file. You can edit this JSON file anytime and upload it here to update everything in one click!
      </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
       {/* Category 1: Full Master Bundle */}
              <div style={{ background: 'rgba(255, 255, 255, 0.03)', padding: 16, borderRadius: 16, border: '1px solid rgba(208, 188, 255, 0.15)' }}>
                <div style={{ fontSize: 13, fontWeight: 800, color: '#d0bcff', marginBottom: 10, display: 'flex', alignItems: 'center', gap: 6 }}>
         <Database size={15} /> Full Master Bundle (All Routines, Exams & Subjects)
        </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 200px), 1fr))', gap: 10 }}>
         <button
          onClick={handleExportFullMasterJSON}
                    style={{
           padding: '12px 16px',
           borderRadius: '12px',
           background: 'rgba(208, 188, 255, 0.18)',
           border: '1px solid rgba(208, 188, 255, 0.35)',
           color: '#d0bcff',
           fontWeight: 800,
           fontSize: 13,
           cursor: 'pointer',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           gap: 6
          }}
         >
          <Download size={16} />
          Download Full Master Bundle
         </button>

         <button
          onClick={handleCopyJson}
                    style={{
           padding: '12px 16px',
           borderRadius: '12px',
           background: 'rgba(255, 255, 255, 0.06)',
           border: '1px solid rgba(255, 255, 255, 0.15)',
           color: '#fff',
           fontWeight: 700,
           fontSize: 13,
           cursor: 'pointer',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           gap: 6
          }}
         >
          <Copy size={16} />
          Copy Master JSON
         </button>
        </div>
       </div>

       {/* Category 2: Class Routines (Offline, Online, Both) */}
              <div style={{ background: 'rgba(255, 255, 255, 0.03)', padding: 16, borderRadius: 16, border: '1px solid rgba(0, 229, 255, 0.15)' }}>
                <div style={{ fontSize: 13, fontWeight: 800, color: '#00e5ff', marginBottom: 10, display: 'flex', alignItems: 'center', gap: 6 }}>
         <BookOpen size={15} /> Class Routine Datasets (Offline & Online)
        </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 200px), 1fr))', gap: 10 }}>
         <button
          onClick={handleExportOfflineRoutineJSON}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: 'rgba(0, 229, 255, 0.12)',
           border: '1px solid rgba(0, 229, 255, 0.3)',
           color: '#00e5ff',
           fontWeight: 800,
           fontSize: 12.5,
           cursor: 'pointer',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           gap: 6
          }}
         >
          <Download size={15} />
          Export Offline Routine ({offlineRoutines.length} Days)
         </button>

         <button
          onClick={handleExportOnlineRoutineJSON}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: 'rgba(0, 229, 255, 0.12)',
           border: '1px solid rgba(0, 229, 255, 0.3)',
           color: '#00e5ff',
           fontWeight: 800,
           fontSize: 12.5,
           cursor: 'pointer',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           gap: 6
          }}
         >
          <Download size={15} />
          Export Online Routine ({onlineRoutines.length} Days)
         </button>

         <button
          onClick={handleExportAllRoutinesJSON}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: 'rgba(0, 229, 255, 0.18)',
           border: '1px solid rgba(0, 229, 255, 0.4)',
           color: '#00e5ff',
           fontWeight: 800,
           fontSize: 12.5,
           cursor: 'pointer',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           gap: 6
          }}
         >
          <Layers size={15} />
          Export All Routines (Both)
         </button>
        </div>
       </div>

       {/* Category 3: Exam Schedules (Offline, Online, Both) */}
              <div style={{ background: 'rgba(255, 255, 255, 0.03)', padding: 16, borderRadius: 16, border: '1px solid rgba(255, 183, 77, 0.15)' }}>
                <div style={{ fontSize: 13, fontWeight: 800, color: '#ffb74d', marginBottom: 10, display: 'flex', alignItems: 'center', gap: 6 }}>
         <Calendar size={15} /> Exam Schedule Datasets (Offline & Online)
        </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 200px), 1fr))', gap: 10 }}>
         <button
          onClick={handleExportOfflineExamsJSON}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: 'rgba(255, 183, 77, 0.12)',
           border: '1px solid rgba(255, 183, 77, 0.3)',
           color: '#ffb74d',
           fontWeight: 800,
           fontSize: 12.5,
           cursor: 'pointer',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           gap: 6
          }}
         >
          <Download size={15} />
          Export Offline Exams ({offlineExams.length} Items)
         </button>

         <button
          onClick={handleExportOnlineExamsJSON}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: 'rgba(255, 183, 77, 0.12)',
           border: '1px solid rgba(255, 183, 77, 0.3)',
           color: '#ffb74d',
           fontWeight: 800,
           fontSize: 12.5,
           cursor: 'pointer',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           gap: 6
          }}
         >
          <Download size={15} />
          Export Online Exams ({onlineExams.length} Items)
         </button>

         <button
          onClick={handleExportAllExamsJSON}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: 'rgba(255, 183, 77, 0.18)',
           border: '1px solid rgba(255, 183, 77, 0.4)',
           color: '#ffb74d',
           fontWeight: 800,
           fontSize: 12.5,
           cursor: 'pointer',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           gap: 6
          }}
         >
          <Layers size={15} />
          Export All Exams (Both)
         </button>
        </div>
       </div>

       {/* Category 4: Universal Import & Restore Actions */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 220px), 1fr))', gap: 12, marginTop: 4 }}>
        {/* Import JSON Button */}
        <button
         onClick={() => fileInputRef.current?.click()}
                  style={{
          padding: '14px 18px',
          borderRadius: '14px',
          background: 'rgba(129, 199, 132, 0.18)',
          border: '1px solid rgba(129, 199, 132, 0.35)',
          color: '#81c784',
          fontWeight: 800,
          fontSize: 13.5,
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 8,
          transition: 'all 0.2s ease'
         }}
        >
         <Upload size={18} />
         Upload & Restore Any JSON File
        </button>

        {/* Reset to Official Schedule */}
        <button
         onClick={handleResetToMasterSchedule}
                  style={{
          padding: '14px 18px',
          borderRadius: '14px',
          background: 'rgba(255, 82, 82, 0.15)',
          border: '1px solid rgba(255, 82, 82, 0.35)',
          color: '#ff5252',
          fontWeight: 800,
          fontSize: 13.5,
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 8,
          transition: 'all 0.2s ease'
         }}
        >
         <RotateCcw size={18} />
         Reset to Official EAP 2026
        </button>
       </div>
      </div>
     </div>
    </div>
   )}

   {/* TAB 5: ADMIN MASTER GUIDE & HELP CENTER */}
   {activeTab === 'help' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
     {/* Header Banner */}
     <div
      className="haze-card"
            style={{
       borderRadius: '24px',
       padding: '28px 24px',
       background: 'linear-gradient(135deg, rgba(208, 188, 255, 0.15) 0%, rgba(124, 77, 255, 0.1) 50%, rgba(0, 229, 255, 0.08) 100%)',
       border: '1.5px solid rgba(208, 188, 255, 0.35)',
       display: 'flex',
       flexDirection: 'column',
       gap: 14,
       boxShadow: '0 12px 36px rgba(0, 0, 0, 0.4)'
      }}
     >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                <div style={{
         width: 52,
         height: 52,
         borderRadius: '16px',
         background: 'linear-gradient(135deg, #d0bcff, #b69df8)',
         display: 'flex',
         alignItems: 'center',
         justifyContent: 'center',
         color: '#381e72',
         boxShadow: '0 4px 18px rgba(208, 188, 255, 0.4)'
        }}>
         <BookMarked size={28} />
        </div>
        <div>
                  <h2 style={{ margin: 0, fontSize: 22, fontWeight: 900, color: '#fff', letterSpacing: '0.3px' }}>
           EAP Master Admin Operations & Field Manual
         </h2>
                  <p style={{ margin: '3px 0 0', fontSize: 13.5, color: 'rgba(255, 255, 255, 0.75)' }}>
          A human-written, complete reference guide explaining every feature, workflow, and sync mechanism with practical examples.
         </p>
        </div>
       </div>

              <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
        <button
         onClick={() => {
          setHelpSearchQuery('');
          setHelpSelectedCategory('all');
          sound.playTick();
         }}
                  style={{
          padding: '8px 14px',
          borderRadius: '10px',
          background: 'rgba(255, 255, 255, 0.08)',
          border: '1px solid rgba(255, 255, 255, 0.15)',
          color: '#fff',
          fontSize: 12,
          fontWeight: 700,
          cursor: 'pointer'
         }}
        >
         Clear Filters
        </button>
       </div>
      </div>

      {/* Quick Search & Category Filters */}
            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'center', marginTop: 4 }}>
              <div style={{ position: 'relative', flex: '1 1 300px' }}>
                <Search size={16} style={{ position: 'absolute', left: 14, top: 12, color: 'rgba(255, 255, 255, 0.4)' }} />
        <input
         type="text"
         value={helpSearchQuery}
         onChange={(e) => setHelpSearchQuery(e.target.value)}
         placeholder="Search guide (e.g., custom routine, presets, countdown date picker, ghost profiles)..."
                  style={{
          width: '100%',
          padding: '10px 14px 10px 38px',
          borderRadius: '12px',
          background: 'rgba(0, 0, 0, 0.35)',
          border: '1px solid rgba(208, 188, 255, 0.3)',
          color: '#fff',
          fontSize: 13,
          outline: 'none'
         }}
        />
       </div>

       {/* Category Pills */}
              <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
        {[
         { id: 'all', label: 'All Topics' },
         { id: 'sync', label: ' Sync Architecture' },
         { id: 'students', label: ' Student Management' },
         { id: 'presets', label: ' Presets & Tracks' },
         { id: 'routines', label: ' Routines & Exams' },
         { id: 'syllabus', label: ' Syllabus Structure' },
         { id: 'countdown', label: ' Target Countdown' },
         { id: 'notifications', label: ' Notifications & Auditor' },
         { id: 'quotes', label: ' Quotes Studio' },
         { id: 'api', label: ' REST API & Backups' }
        ].map((cat) => {
         const isSel = helpSelectedCategory === cat.id;
         return (
          <button
           key={cat.id}
           onClick={() => {
            sound.playTick();
            setHelpSelectedCategory(cat.id);
           }}
                      style={{
            padding: '6px 12px',
            borderRadius: '10px',
            background: isSel ? '#d0bcff' : 'rgba(255, 255, 255, 0.06)',
            color: isSel ? '#381e72' : 'rgba(255, 255, 255, 0.8)',
            border: isSel ? 'none' : '1px solid rgba(255, 255, 255, 0.1)',
            fontWeight: 800,
            fontSize: 11.5,
            cursor: 'pointer'
           }}
          >
           {cat.label}
          </button>
         );
        })}
       </div>
      </div>
     </div>

     {/* Guide Articles Grid */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* ARTICLE 1: CLOUD SYNC & ARCHITECTURE */}
      {(helpSelectedCategory === 'all' || helpSelectedCategory === 'sync') && (
       <div
        className="haze-card"
                style={{
         borderRadius: '20px',
         padding: '24px',
         background: 'rgba(255, 255, 255, 0.03)',
         border: '1px solid rgba(208, 188, 255, 0.2)',
         display: 'flex',
         flexDirection: 'column',
         gap: 14
        }}
       >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div style={{ padding: '6px 10px', borderRadius: 8, background: 'rgba(208, 188, 255, 0.15)', color: '#d0bcff', fontWeight: 900, fontSize: 13 }}>
          Section 1
         </div>
                  <h3 style={{ margin: 0, fontSize: 18, fontWeight: 900, color: '#fff' }}>
           How Real-time Cloud Sync & Overrides Work
         </h3>
        </div>

                <p style={{ margin: 0, fontSize: 13.5, color: 'rgba(255, 255, 255, 0.85)', lineHeight: 1.6 }}>
         The EAP Tracker Admin Panel acts as the central command bridge for all students' Android mobile phones. Changes published here are synced in real time via Firebase Realtime Database (RTDB).
        </p>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 280px), 1fr))', gap: 12 }}>
                  <div style={{ background: 'rgba(0, 229, 255, 0.06)', border: '1px solid rgba(0, 229, 255, 0.2)', borderRadius: 14, padding: 14 }}>
                    <h4 style={{ margin: '0 0 6px', color: '#00e5ff', fontSize: 14, fontWeight: 900 }}>
            Global Master Track (`/master_data_json`)
          </h4>
                    <p style={{ margin: 0, fontSize: 12.5, color: 'rgba(255, 255, 255, 0.75)', lineHeight: 1.5 }}>
           When you click <strong>"Push Master Live to Cloud"</strong> from the Routines, Exams, or Syllabus tabs, it writes to the master database. Every regular student phone automatically downloads this standard curriculum on app startup or background sync.
          </p>
         </div>

                  <div style={{ background: 'rgba(208, 188, 255, 0.06)', border: '1px solid rgba(208, 188, 255, 0.2)', borderRadius: 14, padding: 14 }}>
                    <h4 style={{ margin: '0 0 6px', color: '#d0bcff', fontSize: 14, fontWeight: 900 }}>
            Student Custom Overrides (`/users/[studentId]/...`)
          </h4>
                    <p style={{ margin: 0, fontSize: 12.5, color: 'rgba(255, 255, 255, 0.75)', lineHeight: 1.5 }}>
           If you assign a custom routine, syllabus, or exam schedule to a specific student (e.g. for BUET or Medical admission prep), the student's phone prioritizes their custom data over the global master.
          </p>
         </div>
        </div>

                <div style={{ background: 'rgba(255, 213, 79, 0.06)', border: '1px solid rgba(255, 213, 79, 0.25)', borderRadius: 12, padding: 12, fontSize: 12.5, color: '#ffd54f', display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Lightbulb size={16} style={{ flexShrink: 0 }} />
         <span>
          <strong>Mentor Pro-Tip:</strong> If a student with a custom track wants to switch back to the regular batch schedule, simply open their workspace in <strong>Student Roster</strong> and click <strong>"Reset to Global Master"</strong>.
         </span>
        </div>
       </div>
      )}

      {/* ARTICLE 2: STUDENT MANAGEMENT & WORKSPACES */}
      {(helpSelectedCategory === 'all' || helpSelectedCategory === 'students') && (
       <div
        className="haze-card"
                style={{
         borderRadius: '20px',
         padding: '24px',
         background: 'rgba(255, 255, 255, 0.03)',
         border: '1px solid rgba(208, 188, 255, 0.2)',
         display: 'flex',
         flexDirection: 'column',
         gap: 14
        }}
       >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div style={{ padding: '6px 10px', borderRadius: 8, background: 'rgba(208, 188, 255, 0.15)', color: '#d0bcff', fontWeight: 900, fontSize: 13 }}>
          Section 2
         </div>
                  <h3 style={{ margin: 0, fontSize: 18, fontWeight: 900, color: '#fff' }}>
           Student Roster & Individual Candidate Customization
         </h3>
        </div>

                <p style={{ margin: 0, fontSize: 13.5, color: 'rgba(255, 255, 255, 0.85)', lineHeight: 1.6 }}>
         The <strong>Student Roster</strong> tab provides real-time telemetry from candidate devices: today's study hours, syllabus percentage, total logged exams, live study status, and last active timestamp.
        </p>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                  <h4 style={{ margin: 0, fontSize: 14.5, fontWeight: 900, color: '#d0bcff' }}>
          Step-by-Step: How to Customize an Individual Student's Track
         </h4>
                  <ol style={{ margin: 0, paddingLeft: 20, fontSize: 13, color: 'rgba(255, 255, 255, 0.8)', lineHeight: 1.7 }}>
          <li>Navigate to <strong>Student Roster</strong> (`#admin/students`) and search for the student by name, email, or institution.</li>
          <li>Click <strong>"View & Edit Detail"</strong> on their card to open their full interactive workspace.</li>
          <li>
           Choose the area you want to customize:
                      <ul style={{ paddingLeft: 18, marginTop: 4 }}>
            <li><strong>Custom Syllabus:</strong> Add new subjects (e.g. General Knowledge, ICT), rename chapters, reorder chapters, or delete unused subjects.</li>
            <li><strong>Custom Offline / Online Routine:</strong> Adjust day-by-day classes, topics, and revision modules.</li>
            <li><strong>Custom Exams:</strong> Schedule student-specific model tests and mock exam series.</li>
           </ul>
          </li>
          <li>Click the purple <strong>" Push Custom Live"</strong> button to deploy the changes directly to their phone.</li>
          <li>
           <em>Optional:</em> Click <strong>" Save as Preset Track"</strong> to save this student's tailored routine as a reusable track for other candidates!
          </li>
         </ol>
        </div>

                <div style={{ background: 'rgba(239, 83, 80, 0.08)', border: '1px solid rgba(239, 83, 80, 0.25)', borderRadius: 12, padding: 12 }}>
                  <h5 style={{ margin: '0 0 4px', fontSize: 13, fontWeight: 900, color: '#ef5350' }}>
           What is the "Ghost Profile Detector"?
         </h5>
                  <p style={{ margin: 0, fontSize: 12.5, color: 'rgba(255, 255, 255, 0.75)', lineHeight: 1.5 }}>
          If an auth account was deleted or registered incompletely without student profile data, the admin panel flags it with a yellow ghost indicator. Clicking <strong>"Cleanup Ghost Profiles"</strong> cleans up dangling records safely.
         </p>
        </div>
       </div>
      )}

      {/* ARTICLE 3: PRESETS & TRACK STUDIO */}
      {(helpSelectedCategory === 'all' || helpSelectedCategory === 'presets') && (
       <div
        className="haze-card"
                style={{
         borderRadius: '20px',
         padding: '24px',
         background: 'rgba(255, 255, 255, 0.03)',
         border: '1px solid rgba(208, 188, 255, 0.2)',
         display: 'flex',
         flexDirection: 'column',
         gap: 14
        }}
       >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div style={{ padding: '6px 10px', borderRadius: 8, background: 'rgba(208, 188, 255, 0.15)', color: '#d0bcff', fontWeight: 900, fontSize: 13 }}>
          Section 3
         </div>
                  <h3 style={{ margin: 0, fontSize: 18, fontWeight: 900, color: '#fff' }}>
           Presets & Track Studio (University Blueprints)
         </h3>
        </div>

                <p style={{ margin: 0, fontSize: 13.5, color: 'rgba(255, 255, 255, 0.85)', lineHeight: 1.6 }}>
         Instead of re-creating routines and syllabuses for every new admission batch, you can create and store reusable <strong>Preset Tracks</strong> (e.g. BUET Pure Engineering, Medical Crash, DU KA, IBA, 30-Day Model Test Series).
        </p>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 260px), 1fr))', gap: 12 }}>
                  <div style={{ background: 'rgba(255, 255, 255, 0.03)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: 12, padding: 12 }}>
                    <span style={{ fontSize: 12, fontWeight: 900, color: '#d0bcff' }}>1. Visual Editor</span>
                    <p style={{ margin: '4px 0 0', fontSize: 12, color: 'rgba(255, 255, 255, 0.7)' }}>
           Add/edit subjects, chapters, and routine days using interactive visual cards with + Add, Edit, and Delete buttons.
          </p>
         </div>

                  <div style={{ background: 'rgba(255, 255, 255, 0.03)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: 12, padding: 12 }}>
                    <span style={{ fontSize: 12, fontWeight: 900, color: '#00e5ff' }}>2. Direct Raw JSON Editor</span>
                    <p style={{ margin: '4px 0 0', fontSize: 12, color: 'rgba(255, 255, 255, 0.7)' }}>
           Switch to the "Raw JSON" tab to paste or tweak JSON structures directly, with a built-in "Format / Beautify" tool.
          </p>
         </div>

                  <div style={{ background: 'rgba(255, 255, 255, 0.03)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: 12, padding: 12 }}>
                    <span style={{ fontSize: 12, fontWeight: 900, color: '#ffd54f' }}>3. JSON File Upload / Import</span>
                    <p style={{ margin: '4px 0 0', fontSize: 12, color: 'rgba(255, 255, 255, 0.7)' }}>
           Click "Import JSON Track" to upload any `.json` curriculum or routine file directly from your computer.
          </p>
         </div>

                  <div style={{ background: 'rgba(255, 255, 255, 0.03)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: 12, padding: 12 }}>
                    <span style={{ fontSize: 12, fontWeight: 900, color: '#81c784' }}>4. 1-Click Mass Deploy</span>
                    <p style={{ margin: '4px 0 0', fontSize: 12, color: 'rgba(255, 255, 255, 0.7)' }}>
           Click "Deploy to Students ", select multiple candidates (or all candidates), and push the track to all of them at once.
          </p>
         </div>
        </div>
       </div>
      )}

      {/* ARTICLE 4: ROUTINES & EXAMS */}
      {(helpSelectedCategory === 'all' || helpSelectedCategory === 'routines') && (
       <div
        className="haze-card"
                style={{
         borderRadius: '20px',
         padding: '24px',
         background: 'rgba(255, 255, 255, 0.03)',
         border: '1px solid rgba(208, 188, 255, 0.2)',
         display: 'flex',
         flexDirection: 'column',
         gap: 14
        }}
       >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div style={{ padding: '6px 10px', borderRadius: 8, background: 'rgba(208, 188, 255, 0.15)', color: '#d0bcff', fontWeight: 900, fontSize: 13 }}>
          Section 4
         </div>
                  <h3 style={{ margin: 0, fontSize: 18, fontWeight: 900, color: '#fff' }}>
           Class Routines & Exam Series Management
         </h3>
        </div>

                <p style={{ margin: 0, fontSize: 13.5, color: 'rgba(255, 255, 255, 0.85)', lineHeight: 1.6 }}>
         Both the <strong>Class Routines</strong> and <strong>Exam Schedules</strong> tabs support separate Offline (Physical Batch) and Online (Live Batch) schedules.
        </p>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 8, fontSize: 13, color: 'rgba(255, 255, 255, 0.8)' }}>
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
                    <CheckCircle size={15} color="#d0bcff" style={{ marginTop: 3, flexShrink: 0 }} />
          <span><strong>Calendar Date Picker:</strong> When editing or adding a day, you can pick the date using the native interactive calendar to avoid formatting mistakes.</span>
         </div>
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
                    <CheckCircle size={15} color="#d0bcff" style={{ marginTop: 3, flexShrink: 0 }} />
          <span><strong>Auto Date-Sorting:</strong> Whenever you add or edit a day, the system automatically sorts the routine chronologically by date.</span>
         </div>
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
                    <CheckCircle size={15} color="#d0bcff" style={{ marginTop: 3, flexShrink: 0 }} />
          <span><strong>Universal Save as Preset:</strong> Click the yellow <em>" Save as Preset"</em> button in the header to store the active routine or exam series as a reusable track template.</span>
         </div>
        </div>
       </div>
      )}

      {/* ARTICLE 5: SYLLABUS MASTER */}
      {(helpSelectedCategory === 'all' || helpSelectedCategory === 'syllabus') && (
       <div
        className="haze-card"
                style={{
         borderRadius: '20px',
         padding: '24px',
         background: 'rgba(255, 255, 255, 0.03)',
         border: '1px solid rgba(208, 188, 255, 0.2)',
         display: 'flex',
         flexDirection: 'column',
         gap: 14
        }}
       >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div style={{ padding: '6px 10px', borderRadius: 8, background: 'rgba(208, 188, 255, 0.15)', color: '#d0bcff', fontWeight: 900, fontSize: 13 }}>
          Section 5
         </div>
                  <h3 style={{ margin: 0, fontSize: 18, fontWeight: 900, color: '#fff' }}>
           Syllabus & Subject Hierarchy Architecture
         </h3>
        </div>

                <p style={{ margin: 0, fontSize: 13.5, color: 'rgba(255, 255, 255, 0.85)', lineHeight: 1.6 }}>
         The syllabus follows a 4-tier structural model designed for Bangladesh Higher Secondary & University Admission tracking:
        </p>

                <div style={{ background: 'rgba(0, 0, 0, 0.3)', borderRadius: 14, padding: 16, border: '1px solid rgba(255, 255, 255, 0.08)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', fontSize: 13, fontWeight: 800 }}>
                    <span style={{ color: '#d0bcff' }}>1. Subject (e.g. Physics)</span>
                    <span style={{ color: 'rgba(255,255,255,0.4)' }}></span>
                    <span style={{ color: '#00e5ff' }}>2. Paper (1st Paper / 2nd Paper)</span>
                    <span style={{ color: 'rgba(255,255,255,0.4)' }}></span>
                    <span style={{ color: '#ffd54f' }}>3. Chapter (e.g. গতিবিদ্যা)</span>
                    <span style={{ color: 'rgba(255,255,255,0.4)' }}></span>
                    <span style={{ color: '#81c784' }}>4. Milestones (বই রিডিং, ক্লাস, প্রশ্নব্যাংক, কনসেপ্ট)</span>
         </div>
        </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 8, fontSize: 13, color: 'rgba(255, 255, 255, 0.8)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Edit2 size={14} color="#d0bcff" />
          <span><strong>Rename Chapter:</strong> Click the edit icon next to any chapter name to rename it instantly using the dark glass prompt dialog.</span>
         </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <ArrowUpDown size={14} color="#00e5ff" />
          <span><strong>Reorder Chapters:</strong> Use the Up/Down arrow buttons to adjust the sequence of chapters within any paper.</span>
         </div>
        </div>
       </div>
      )}

      {/* ARTICLE 6: TARGET EXAM COUNTDOWN */}
      {(helpSelectedCategory === 'all' || helpSelectedCategory === 'countdown') && (
       <div
        className="haze-card"
                style={{
         borderRadius: '20px',
         padding: '24px',
         background: 'rgba(255, 255, 255, 0.03)',
         border: '1px solid rgba(208, 188, 255, 0.2)',
         display: 'flex',
         flexDirection: 'column',
         gap: 14
        }}
       >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div style={{ padding: '6px 10px', borderRadius: 8, background: 'rgba(208, 188, 255, 0.15)', color: '#d0bcff', fontWeight: 900, fontSize: 13 }}>
          Section 6
         </div>
                  <h3 style={{ margin: 0, fontSize: 18, fontWeight: 900, color: '#fff' }}>
           Target Countdown & Exam Calendar Engine
         </h3>
        </div>

                <p style={{ margin: 0, fontSize: 13.5, color: 'rgba(255, 255, 255, 0.85)', lineHeight: 1.6 }}>
         The <strong>Target Exam Countdown Manager</strong> sets the primary admission test (e.g. BUET, Medical, DU KA) displayed on candidates' mobile home screens.
        </p>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 260px), 1fr))', gap: 12 }}>
                  <div style={{ background: 'rgba(0, 229, 255, 0.06)', borderRadius: 12, padding: 12, border: '1px solid rgba(0, 229, 255, 0.2)' }}>
                    <span style={{ fontSize: 13, fontWeight: 900, color: '#00e5ff' }}>Calendar Date Picker</span>
                    <p style={{ margin: '4px 0 0', fontSize: 12, color: 'rgba(255, 255, 255, 0.75)' }}>
           Pick the exact exam date with the calendar tool. The system automatically converts it to <code>DD-MMM-YYYY</code> and calculates remaining days.
          </p>
         </div>

                  <div style={{ background: 'rgba(239, 83, 80, 0.06)', borderRadius: 12, padding: 12, border: '1px solid rgba(239, 83, 80, 0.2)' }}>
                    <span style={{ fontSize: 13, fontWeight: 900, color: '#ef5350' }}>1-Click Clear / Reset</span>
                    <p style={{ margin: '4px 0 0', fontSize: 12, color: 'rgba(255, 255, 255, 0.75)' }}>
           Click <strong>"Clear / Reset"</strong> to publish <code>{'{ cleared: true }'}</code> to Firebase RTDB. Candidate phones immediately detect this and revert to standard mode without any stale countdowns.
          </p>
         </div>
        </div>
       </div>
      )}

      {/* ARTICLE 7: NOTIFICATIONS & INBOX AUDITOR */}
      {(helpSelectedCategory === 'all' || helpSelectedCategory === 'notifications') && (
       <div
        className="haze-card"
                style={{
         borderRadius: '20px',
         padding: '24px',
         background: 'rgba(255, 255, 255, 0.03)',
         border: '1px solid rgba(208, 188, 255, 0.2)',
         display: 'flex',
         flexDirection: 'column',
         gap: 14
        }}
       >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div style={{ padding: '6px 10px', borderRadius: 8, background: 'rgba(208, 188, 255, 0.15)', color: '#d0bcff', fontWeight: 900, fontSize: 13 }}>
          Section 7
         </div>
                  <h3 style={{ margin: 0, fontSize: 18, fontWeight: 900, color: '#fff' }}>
           Push Broadcasts & Live Notification Inbox Auditor
         </h3>
        </div>

                <p style={{ margin: 0, fontSize: 13.5, color: 'rgba(255, 255, 255, 0.85)', lineHeight: 1.6 }}>
         Broadcast instant high-priority alerts to all candidate devices via Firebase Cloud Messaging (FCM), and inspect individual student inboxes.
        </p>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                  <div style={{ background: 'rgba(255, 255, 255, 0.03)', borderRadius: 12, padding: 12, border: '1px solid rgba(255, 255, 255, 0.08)' }}>
                    <h5 style={{ margin: '0 0 4px', fontSize: 13.5, fontWeight: 900, color: '#d0bcff' }}>
           Live Student Notification Inbox Auditor
          </h5>
                    <p style={{ margin: 0, fontSize: 12.5, color: 'rgba(255, 255, 255, 0.75)', lineHeight: 1.5 }}>
           In the <strong>Notifications Hub</strong> tab, select any registered candidate from the dropdown. The auditor loads their live in-app notification inbox. You can inspect delivered notices and delete any outdated notice directly from Firebase RTDB with 1 click.
          </p>
         </div>
        </div>
       </div>
      )}

      {/* ARTICLE 8: REST API & BACKUPS */}
      {(helpSelectedCategory === 'all' || helpSelectedCategory === 'api') && (
       <div
        className="haze-card"
                style={{
         borderRadius: '20px',
         padding: '24px',
         background: 'rgba(255, 255, 255, 0.03)',
         border: '1px solid rgba(208, 188, 255, 0.2)',
         display: 'flex',
         flexDirection: 'column',
         gap: 14
        }}
       >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div style={{ padding: '6px 10px', borderRadius: 8, background: 'rgba(208, 188, 255, 0.15)', color: '#d0bcff', fontWeight: 900, fontSize: 13 }}>
          Section 8
         </div>
                  <h3 style={{ margin: 0, fontSize: 18, fontWeight: 900, color: '#fff' }}>
           REST API Hub & 7-in-1 Export Hub Backup System
         </h3>
        </div>

                <p style={{ margin: 0, fontSize: 13.5, color: 'rgba(255, 255, 255, 0.85)', lineHeight: 1.6 }}>
         Keep full control over your database with direct REST endpoint URLs and comprehensive JSON backup downloads.
        </p>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 260px), 1fr))', gap: 10 }}>
                  <div style={{ background: 'rgba(0, 0, 0, 0.3)', borderRadius: 10, padding: 10, fontSize: 12, fontFamily: 'monospace', color: '#00e5ff' }}>
          /master_data_json.json
         </div>
                  <div style={{ background: 'rgba(0, 0, 0, 0.3)', borderRadius: 10, padding: 10, fontSize: 12, fontFamily: 'monospace', color: '#00e5ff' }}>
          /target_exam_config.json
         </div>
                  <div style={{ background: 'rgba(0, 0, 0, 0.3)', borderRadius: 10, padding: 10, fontSize: 12, fontFamily: 'monospace', color: '#00e5ff' }}>
          /announcements.json
         </div>
                  <div style={{ background: 'rgba(0, 0, 0, 0.3)', borderRadius: 10, padding: 10, fontSize: 12, fontFamily: 'monospace', color: '#00e5ff' }}>
          /users.json
         </div>
        </div>

                <div style={{ background: 'rgba(129, 199, 132, 0.08)', border: '1px solid rgba(129, 199, 132, 0.25)', borderRadius: 12, padding: 12, fontSize: 12.5, color: '#81c784' }}>
         <strong>Disaster Recovery:</strong> Click the top-bar <strong>"Export Hub (7 Options)"</strong> to download full offline JSON snapshots of your entire database anytime.
        </div>
       </div>
      )}
     </div>
    </div>
   )}

   {/* Routine Day Add/Edit Modal */}
   {(isAddingRoutine || editingRoutineItem) && (
    <RoutineEditModal
     initialItem={editingRoutineItem?.item}
     onSave={(item) => {
      // Helper: parse display date "22-Aug-26" → comparable epoch
      const parseDateEpoch = (d: string): number => {
       const months: Record<string, number> = {
        Jan: 0, Feb: 1, Mar: 2, Apr: 3, May: 4, Jun: 5,
        Jul: 6, Aug: 7, Sep: 8, Oct: 9, Nov: 10, Dec: 11
       };
       const m = d.match(/^(\d{1,2})-([A-Za-z]{3})-(\d{2,4})$/);
       if (!m) return 0;
       const yr = m[3].length === 2 ? 2000 + Number(m[3]) : Number(m[3]);
       return new Date(yr, months[m[2]] ?? 0, Number(m[1])).getTime();
      };
      const sortByDate = (arr: RoutineItem[]) =>
       [...arr].sort((a, b) => parseDateEpoch(a.date) - parseDateEpoch(b.date));

      if (editingRoutineItem) {
       if (routineMode === 'Offline') {
        setOfflineRoutines((prev) => sortByDate(prev.map((r, i) => (i === editingRoutineItem.index ? item : r))));
       } else {
        setOnlineRoutines((prev) => sortByDate(prev.map((r, i) => (i === editingRoutineItem.index ? item : r))));
       }
       setEditingRoutineItem(null);
      } else {
       if (routineMode === 'Offline') {
        setOfflineRoutines((prev) => sortByDate([...prev, item]));
       } else {
        setOnlineRoutines((prev) => sortByDate([...prev, item]));
       }
       setIsAddingRoutine(false);
      }
      showNotification('Routine day saved & sorted by date!');
     }}
     onDismiss={() => {
      setIsAddingRoutine(false);
      setEditingRoutineItem(null);
     }}
    />
   )}

   {/* Exam Schedule Add/Edit Modal */}
   {(isAddingExam || editingExamItem) && (
    <ExamEditModal
     initialItem={editingExamItem?.item}
     onSave={(item) => {
      // Sort helper — same pattern as routine
      const parseDateEpoch = (d: string): number => {
       const months: Record<string, number> = {
        Jan: 0, Feb: 1, Mar: 2, Apr: 3, May: 4, Jun: 5,
        Jul: 6, Aug: 7, Sep: 8, Oct: 9, Nov: 10, Dec: 11
       };
       const m = d.match(/^(\d{1,2})-([A-Za-z]{3})-(\d{2,4})$/);
       if (!m) return 0;
       const yr = m[3].length === 2 ? 2000 + Number(m[3]) : Number(m[3]);
       return new Date(yr, months[m[2]] ?? 0, Number(m[1])).getTime();
      };
      const sortByDate = (arr: any[]) =>
       [...arr].sort((a, b) => parseDateEpoch(a.date) - parseDateEpoch(b.date));

      if (editingExamItem) {
       if (examMode === 'Offline') {
        setOfflineExams((prev) => sortByDate(prev.map((r, i) => (i === editingExamItem.index ? item : r))));
       } else {
        setOnlineExams((prev) => sortByDate(prev.map((r, i) => (i === editingExamItem.index ? item : r))));
       }
       setEditingExamItem(null);
      } else {
       if (examMode === 'Offline') {
        setOfflineExams((prev) => sortByDate([...prev, item]));
       } else {
        setOnlineExams((prev) => sortByDate([...prev, item]));
       }
       setIsAddingExam(false);
      }
      showNotification('Exam schedule saved & sorted by date!');
     }}
     onDismiss={() => {
      setIsAddingExam(false);
      setEditingExamItem(null);
     }}
    />
   )}

   {/* Add Subject Modal */}
   {isAddingSubject && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 20 }}>
          <div className="haze-card" style={{ maxWidth: 400, width: '100%', borderRadius: 24, padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }}>
            <h3 style={{ margin: 0, color: '#fff', fontSize: 18, fontWeight: 800 }}>Add New Subject</h3>
      <input
       type="text"
       placeholder="Subject Name (e.g. ICT, English)"
       value={newSubName}
       onChange={(e) => setNewSubName(e.target.value)}
              style={{ padding: 12, borderRadius: 12, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.2)', color: '#fff' }}
      />
            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
              <button onClick={() => setIsAddingSubject(false)} style={{ padding: '8px 16px', borderRadius: 10, background: 'transparent', border: '1px solid rgba(255,255,255,0.2)', color: '#fff', cursor: 'pointer' }}>
        Cancel
       </button>
       <button
        onClick={() => {
         if (newSubName.trim()) {
          const newSub: Subject = {
           id: `sub-${Date.now()}`,
           name: newSubName.trim(),
           iconName: 'Book',
           papers: [
            { id: `pap-1-${Date.now()}`, name: '1st Paper', chapters: [] },
            { id: `pap-2-${Date.now()}`, name: '2nd Paper', chapters: [] }
           ]
          };
          setSubjects((prev) => [...prev, newSub]);
          setNewSubName('');
          setIsAddingSubject(false);
          showNotification('Subject added');
         }
        }}
                style={{ padding: '8px 18px', borderRadius: 10, background: '#d0bcff', color: '#381e72', border: 'none', fontWeight: 800, cursor: 'pointer' }}
       >
        Add Subject
       </button>
      </div>
     </div>
    </div>
   )}

   {/* Add Chapter Modal */}
   {chapterTarget && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 20 }}>
          <div className="haze-card" style={{ maxWidth: 400, width: '100%', borderRadius: 24, padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }}>
            <h3 style={{ margin: 0, color: '#fff', fontSize: 18, fontWeight: 800 }}>Add Chapter</h3>
      <input
       type="text"
       placeholder="Chapter Title (e.g. গতিবিদ্যা)"
       value={newChapterName}
       onChange={(e) => setNewChapterName(e.target.value)}
              style={{ padding: 12, borderRadius: 12, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.2)', color: '#fff' }}
      />
            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
              <button onClick={() => setChapterTarget(null)} style={{ padding: '8px 16px', borderRadius: 10, background: 'transparent', border: '1px solid rgba(255,255,255,0.2)', color: '#fff', cursor: 'pointer' }}>
        Cancel
       </button>
       <button
        onClick={() => {
         if (newChapterName.trim()) {
          const newChap: import('../types').Chapter = {
           id: `chap-${Date.now()}`,
           name: newChapterName.trim(),
           sections: [
            { name: 'Book', label: 'বই রিডিং', isCompleted: false },
            { name: 'Slide', label: 'ক্লাস / স্লাইড', isCompleted: false },
            { name: 'QB', label: 'প্রশ্নব্যাংক', isCompleted: false },
            { name: 'Concept', label: 'কনসেপ্ট / প্র্যাকটিস', isCompleted: false }
           ]
          };
          setSubjects((prev) =>
           prev.map((s) => {
            if (s.id !== chapterTarget.subId) return s;
            return {
             ...s,
             papers: s.papers.map((p) => {
              if (p.id !== chapterTarget.paperId) return p;
              return {
               ...p,
               chapters: [...p.chapters, newChap]
              };
             })
            };
           })
          );
          setNewChapterName('');
          setChapterTarget(null);
          showNotification('Chapter added');
         }
        }}
                style={{ padding: '8px 18px', borderRadius: 10, background: '#d0bcff', color: '#381e72', border: 'none', fontWeight: 800, cursor: 'pointer' }}
       >
        Add Chapter
       </button>
      </div>
     </div>
    </div>
   )}

   {/* Uploaded Routine Preview & Dispatcher Modal */}
   {isUploadingRoutineModalOpen && uploadedRoutinePreview && (
        <div style={{
     position: 'fixed',
     inset: 0,
     background: 'rgba(0,0,0,0.85)',
     zIndex: 9999,
     display: 'flex',
     alignItems: 'center',
     justifyContent: 'center',
     padding: 20,
     backdropFilter: 'blur(10px)'
    }}>
          <div className="haze-card" style={{
      maxWidth: 720,
      width: '100%',
      maxHeight: '90vh',
      borderRadius: 24,
      padding: 24,
      display: 'flex',
      flexDirection: 'column',
      gap: 16,
      overflow: 'hidden'
     }}>
      {/* Modal Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <FileUp size={22} color="#00e5ff" />
        <div>
                  <h3 style={{ margin: 0, color: '#fff', fontSize: 18, fontWeight: 900 }}>
          Preview Uploaded Routine JSON
         </h3>
                  <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.6)' }}>
          File: {uploadedRoutineFileName} • {uploadedRoutinePreview.length} Days Parsed
         </span>
        </div>
       </div>
       <button
        onClick={() => {
         setIsUploadingRoutineModalOpen(false);
         setUploadedRoutinePreview(null);
        }}
                style={{ background: 'transparent', border: 'none', color: 'rgba(255,255,255,0.6)', cursor: 'pointer' }}
       >
        <X size={20} />
       </button>
      </div>

      {/* Target Mode Selector */}
            <div style={{
       display: 'flex',
       flexDirection: 'column',
       gap: 8,
       background: 'rgba(255,255,255,0.04)',
       padding: 14,
       borderRadius: 14,
       border: '1px solid rgba(208, 188, 255, 0.15)'
      }}>
              <label style={{ fontSize: 12, fontWeight: 800, color: '#d0bcff' }}>
        Deploy Destination Target:
       </label>
              <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
        {(['Offline', 'Online', 'Individual'] as const).map((mode) => (
         <label
          key={mode}
                    style={{
           display: 'flex',
           alignItems: 'center',
           gap: 6,
           fontSize: 13,
           color: uploadedRoutineTargetMode === mode ? '#00e5ff' : 'rgba(255,255,255,0.7)',
           cursor: 'pointer',
           fontWeight: uploadedRoutineTargetMode === mode ? 800 : 500,
           background: uploadedRoutineTargetMode === mode ? 'rgba(0, 229, 255, 0.12)' : 'transparent',
           padding: '6px 12px',
           borderRadius: 8
          }}
         >
          <input
           type="radio"
           name="routineTargetMode"
           checked={uploadedRoutineTargetMode === mode}
           onChange={() => setUploadedRoutineTargetMode(mode)}
          />
          <span>
           {mode === 'Offline' && 'Master Offline Routine (All Offline Students)'}
           {mode === 'Online' && 'Master Online Routine (All Online Students)'}
           {mode === 'Individual' && 'Specific Individual Student by ID'}
          </span>
         </label>
        ))}
       </div>

       {uploadedRoutineTargetMode === 'Individual' && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 4, marginTop: 6 }}>
                  <label style={{ fontSize: 11, fontWeight: 700, color: '#00e5ff' }}>
          Enter or Select Student ID:
         </label>
                  <div style={{ display: 'flex', gap: 8 }}>
          <input
           type="text"
           placeholder="e.g. EAP-2025-042"
           value={uploadedRoutineTargetStudentId}
           onChange={(e) => setUploadedRoutineTargetStudentId(e.target.value)}
                      style={{
            flex: 1,
            padding: '10px 12px',
            borderRadius: 10,
            background: 'rgba(255,255,255,0.06)',
            border: '1px solid rgba(0, 229, 255, 0.3)',
            color: '#fff',
            fontSize: 13
           }}
          />
          {studentsList.length > 0 && (
           <select
            onChange={(e) => setUploadedRoutineTargetStudentId(e.target.value)}
            value={uploadedRoutineTargetStudentId}
                        style={{
             padding: '10px 12px',
             borderRadius: 10,
             background: '#2b2930',
             border: '1px solid rgba(0, 229, 255, 0.3)',
             color: '#fff',
             fontSize: 13
            }}
           >
            <option value="">-- Quick Pick Student --</option>
            {studentsList.map((s, idx) => (
             <option key={idx} value={s.studentId}>
              {s.name} ({s.studentId}) - {s.college}
             </option>
            ))}
           </select>
          )}
         </div>
        </div>
       )}
      </div>

      {/* Scrollable Routine Preview Table */}
            <div style={{
       flex: 1,
       overflowY: 'auto',
       maxHeight: 320,
       display: 'flex',
       flexDirection: 'column',
       gap: 8,
       paddingRight: 4
      }}>
       {uploadedRoutinePreview.map((item, idx) => {
        const dayNum = (item as any).dayNumber || (idx + 1);
        const isRest = (item as any).isRestDay || (item.classSubject?.toLowerCase().includes('rest') || item.classSubject?.toLowerCase().includes('self study'));
        return (
         <div
          key={idx}
                    style={{
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'space-between',
           padding: '10px 14px',
           borderRadius: 12,
           background: isRest ? 'rgba(255, 183, 77, 0.08)' : 'rgba(255,255,255,0.04)',
           border: '1px solid rgba(255,255,255,0.08)'
          }}
         >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                      <span style={{
            fontSize: 12,
            fontWeight: 900,
            color: isRest ? '#ffb74d' : '#d0bcff',
            background: isRest ? 'rgba(255, 183, 77, 0.15)' : 'rgba(208, 188, 255, 0.15)',
            padding: '4px 8px',
            borderRadius: 8,
            minWidth: 54,
            textAlign: 'center'
           }}>
            Day {dayNum}
           </span>
           <div>
                        <div style={{ fontSize: 13, fontWeight: 800, color: '#fff', display: 'flex', alignItems: 'center', gap: 6 }}>
             {isRest ? (
              <>
               <Sparkles size={13} color="#ffb74d" />
               <span>Rest & Self Study Day</span>
              </>
             ) : (
              <span>{item.classSubject || (item as any).subject || 'Class Session'}</span>
             )}
            </div>
                        <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.6)' }}>
             {item.date || (item as any).dateStr} • {item.day || (item as any).dayName} {item.topics && item.topics.length > 0 ? `• ${Array.isArray(item.topics) ? item.topics.join(', ') : item.topics}` : ''}
            </div>
           </div>
          </div>

          {(item.examDetails || (item as any).isExamDay) && (
                      <span style={{
            fontSize: 11,
            fontWeight: 800,
            background: 'rgba(239, 83, 80, 0.15)',
            color: '#ef5350',
            padding: '3px 8px',
            borderRadius: 6
           }}>
            {item.examDetails || (item as any).examType || 'Exam'}
           </span>
          )}
         </div>
        );
       })}
      </div>

      {/* Modal Actions */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 6 }}>
       <button
        onClick={() => {
         setIsUploadingRoutineModalOpen(false);
         setUploadedRoutinePreview(null);
        }}
                style={{
         padding: '10px 18px',
         borderRadius: 10,
         background: 'transparent',
         border: '1px solid rgba(255,255,255,0.2)',
         color: '#fff',
         cursor: 'pointer',
         fontWeight: 700
        }}
       >
        Cancel
       </button>

       <button
        onClick={handleApplyUploadedRoutine}
        disabled={isSyncing}
                style={{
         padding: '10px 22px',
         borderRadius: 10,
         background: 'linear-gradient(135deg, #00e5ff 0%, #00b0ff 100%)',
         color: '#002233',
         border: 'none',
         fontWeight: 900,
         fontSize: 13,
         cursor: isSyncing ? 'default' : 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 8
        }}
       >
        <CloudUpload size={16} />
        <span>
         {uploadedRoutineTargetMode === 'Individual' ? 'Deploy Custom Routine to Student' : `Apply & Update ${uploadedRoutineTargetMode} Routine`}
        </span>
       </button>
      </div>
     </div>
    </div>
   )}

   {/* Personal Student Notice Modal */}
   {isPersonalNoticeModalOpen && (
        <div style={{
     position: 'fixed',
     inset: 0,
     background: 'rgba(0,0,0,0.85)',
     zIndex: 9999,
     display: 'flex',
     alignItems: 'center',
     justifyContent: 'center',
     padding: 20,
     backdropFilter: 'blur(10px)'
    }}>
          <div className="haze-card" style={{
      maxWidth: 480,
      width: '100%',
      borderRadius: 24,
      padding: 24,
      display: 'flex',
      flexDirection: 'column',
      gap: 16
     }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
       <div>
                <h3 style={{ margin: 0, color: '#fff', fontSize: 18, fontWeight: 900 }}>
         Send Mentor Notice
        </h3>
                <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.6)' }}>
         To: {personalNoticeStudentName} ({personalNoticeStudentId})
        </span>
       </div>
       <button
        onClick={() => setIsPersonalNoticeModalOpen(false)}
                style={{ background: 'transparent', border: 'none', color: 'rgba(255,255,255,0.6)', cursor: 'pointer' }}
       >
        <X size={20} />
       </button>
      </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              <label style={{ fontSize: 12, fontWeight: 700, color: '#d0bcff' }}>Notice Title</label>
       <input
        type="text"
        placeholder="e.g. Mentor Study Feedback"
        value={personalNoticeTitle}
        onChange={(e) => setPersonalNoticeTitle(e.target.value)}
                style={{ padding: 12, borderRadius: 12, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(208, 188, 255, 0.25)', color: '#fff', fontSize: 13 }}
       />
      </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              <label style={{ fontSize: 12, fontWeight: 700, color: '#d0bcff' }}>Message Body</label>
       <textarea
        rows={3}
        placeholder="Write private mentor guidance or notice..."
        value={personalNoticeMessage}
        onChange={(e) => setPersonalNoticeMessage(e.target.value)}
                style={{ padding: 12, borderRadius: 12, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(208, 188, 255, 0.25)', color: '#fff', fontSize: 13, resize: 'none' }}
       />
      </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              <label style={{ fontSize: 12, fontWeight: 700, color: '#d0bcff' }}>Resource Link URL (Optional)</label>
       <input
        type="url"
        placeholder="https://drive.google.com/..."
        value={personalNoticeUrl}
        onChange={(e) => setPersonalNoticeUrl(e.target.value)}
                style={{ padding: 12, borderRadius: 12, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(208, 188, 255, 0.25)', color: '#fff', fontSize: 13 }}
       />
      </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              <label style={{ fontSize: 12, fontWeight: 700, color: '#d0bcff' }}>Action Button Label (Optional)</label>
       <input
        type="text"
        placeholder="e.g. Open Class Notes ↗"
        value={personalNoticeButtonText}
        onChange={(e) => setPersonalNoticeButtonText(e.target.value)}
                style={{ padding: 12, borderRadius: 12, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(208, 188, 255, 0.25)', color: '#fff', fontSize: 13 }}
       />
      </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 8 }}>
       <button
        onClick={() => setIsPersonalNoticeModalOpen(false)}
                style={{ padding: '10px 18px', borderRadius: 10, background: 'transparent', border: '1px solid rgba(255,255,255,0.2)', color: '#fff', cursor: 'pointer' }}
       >
        Cancel
       </button>
       <button
        onClick={handleSendPersonalNotice}
        disabled={isSyncing}
                style={{ padding: '10px 20px', borderRadius: 10, background: '#d0bcff', color: '#381e72', border: 'none', fontWeight: 800, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6 }}
       >
        <Send size={15} />
        <span>Send to Student</span>
       </button>
      </div>
     </div>
    </div>
   )}

   {/* EAP Tracker Data Export Hub Modal */}
   {isExportHubModalOpen && (
        <div style={{
     position: 'fixed',
     inset: 0,
     background: 'rgba(0,0,0,0.85)',
     zIndex: 9999,
     display: 'flex',
     alignItems: 'center',
     justifyContent: 'center',
     padding: 20,
     backdropFilter: 'blur(12px)'
    }}>
          <div className="haze-card" style={{
      maxWidth: 720,
      width: '100%',
      maxHeight: '90vh',
      borderRadius: 24,
      padding: 24,
      display: 'flex',
      flexDirection: 'column',
      gap: 16,
      overflowY: 'auto'
     }}>
      {/* Modal Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div style={{
         width: 40,
         height: 40,
         borderRadius: 12,
         background: 'rgba(208, 188, 255, 0.2)',
         display: 'flex',
         alignItems: 'center',
         justifyContent: 'center',
         color: '#d0bcff'
        }}>
         <Download size={20} />
        </div>
        <div>
                  <h3 style={{ margin: 0, color: '#fff', fontSize: 18, fontWeight: 900 }}>
          EAP Tracker Data Export Hub
         </h3>
                  <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.6)' }}>
          Export individual offline/online schedules or download the entire master backup
         </span>
        </div>
       </div>
       <button
        onClick={() => setIsExportHubModalOpen(false)}
                style={{ background: 'transparent', border: 'none', color: 'rgba(255,255,255,0.6)', cursor: 'pointer' }}
       >
        <X size={20} />
       </button>
      </div>

      {/* Export Categories */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
       {/* 1. Full Master Bundle */}
              <div style={{
        background: 'linear-gradient(135deg, rgba(208, 188, 255, 0.12), rgba(124, 77, 255, 0.08))',
        padding: 16,
        borderRadius: 16,
        border: '1px solid rgba(208, 188, 255, 0.3)'
       }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
         <div>
                    <h4 style={{ margin: '0 0 4px 0', fontSize: 15, fontWeight: 800, color: '#d0bcff', display: 'flex', alignItems: 'center', gap: 6 }}>
           <Database size={16} color="#d0bcff" />
           <span>Complete Master Bundle (All Datasets)</span>
          </h4>
                    <p style={{ margin: 0, fontSize: 12, color: 'rgba(255,255,255,0.7)' }}>
           Includes Offline Routine ({offlineRoutines.length}d), Online Routine ({onlineRoutines.length}d), Offline Exams ({offlineExams.length}), Online Exams ({onlineExams.length}), and syllabus topics.
          </p>
         </div>
         <button
          onClick={() => {
           handleExportFullMasterJSON();
           setIsExportHubModalOpen(false);
          }}
                    style={{
           padding: '10px 18px',
           borderRadius: '12px',
           background: '#d0bcff',
           color: '#381e72',
           border: 'none',
           fontWeight: 800,
           fontSize: 13,
           cursor: 'pointer',
           display: 'flex',
           alignItems: 'center',
           gap: 6
          }}
         >
          <Download size={15} />
          Download Master Bundle (.json)
         </button>
        </div>
       </div>

       {/* 2. Class Routine Schedules */}
              <div style={{ background: 'rgba(255, 255, 255, 0.03)', padding: 16, borderRadius: 16, border: '1px solid rgba(0, 229, 255, 0.2)' }}>
                <h4 style={{ margin: '0 0 10px 0', fontSize: 14, fontWeight: 800, color: '#00e5ff', display: 'flex', alignItems: 'center', gap: 6 }}>
         <BookOpen size={16} /> Class Routine Schedules
        </h4>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))', gap: 10 }}>
         <button
          onClick={() => {
           handleExportOfflineRoutineJSON();
           setIsExportHubModalOpen(false);
          }}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: 'rgba(0, 229, 255, 0.12)',
           border: '1px solid rgba(0, 229, 255, 0.3)',
           color: '#00e5ff',
           fontWeight: 800,
           fontSize: 12.5,
           cursor: 'pointer',
           display: 'flex',
           flexDirection: 'column',
           alignItems: 'center',
           gap: 4
          }}
         >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
           <Download size={14} />
           <span>Offline Routine Only</span>
          </div>
                    <span style={{ fontSize: 11, opacity: 0.75 }}>{offlineRoutines.length} Days Routine</span>
         </button>

         <button
          onClick={() => {
           handleExportOnlineRoutineJSON();
           setIsExportHubModalOpen(false);
          }}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: 'rgba(0, 229, 255, 0.12)',
           border: '1px solid rgba(0, 229, 255, 0.3)',
           color: '#00e5ff',
           fontWeight: 800,
           fontSize: 12.5,
           cursor: 'pointer',
           display: 'flex',
           flexDirection: 'column',
           alignItems: 'center',
           gap: 4
          }}
         >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
           <Download size={14} />
           <span>Online Routine Only</span>
          </div>
                    <span style={{ fontSize: 11, opacity: 0.75 }}>{onlineRoutines.length} Days Routine</span>
         </button>

         <button
          onClick={() => {
           handleExportAllRoutinesJSON();
           setIsExportHubModalOpen(false);
          }}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: 'rgba(0, 229, 255, 0.2)',
           border: '1px solid rgba(0, 229, 255, 0.45)',
           color: '#00e5ff',
           fontWeight: 800,
           fontSize: 12.5,
           cursor: 'pointer',
           display: 'flex',
           flexDirection: 'column',
           alignItems: 'center',
           gap: 4
          }}
         >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
           <Layers size={14} />
           <span>Both Class Routines</span>
          </div>
                    <span style={{ fontSize: 11, opacity: 0.75 }}>Offline + Online</span>
         </button>
        </div>
       </div>

       {/* 3. Exam Schedules */}
              <div style={{ background: 'rgba(255, 255, 255, 0.03)', padding: 16, borderRadius: 16, border: '1px solid rgba(255, 183, 77, 0.2)' }}>
                <h4 style={{ margin: '0 0 10px 0', fontSize: 14, fontWeight: 800, color: '#ffb74d', display: 'flex', alignItems: 'center', gap: 6 }}>
         <Calendar size={16} /> Exam Schedules
        </h4>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 180px), 1fr))', gap: 10 }}>
         <button
          onClick={() => {
           handleExportOfflineExamsJSON();
           setIsExportHubModalOpen(false);
          }}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: 'rgba(255, 183, 77, 0.12)',
           border: '1px solid rgba(255, 183, 77, 0.3)',
           color: '#ffb74d',
           fontWeight: 800,
           fontSize: 12.5,
           cursor: 'pointer',
           display: 'flex',
           flexDirection: 'column',
           alignItems: 'center',
           gap: 4
          }}
         >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
           <Download size={14} />
           <span>Offline Exams Only</span>
          </div>
                    <span style={{ fontSize: 11, opacity: 0.75 }}>{offlineExams.length} Scheduled Items</span>
         </button>

         <button
          onClick={() => {
           handleExportOnlineExamsJSON();
           setIsExportHubModalOpen(false);
          }}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: 'rgba(255, 183, 77, 0.12)',
           border: '1px solid rgba(255, 183, 77, 0.3)',
           color: '#ffb74d',
           fontWeight: 800,
           fontSize: 12.5,
           cursor: 'pointer',
           display: 'flex',
           flexDirection: 'column',
           alignItems: 'center',
           gap: 4
          }}
         >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
           <Download size={14} />
           <span>Online Exams Only</span>
          </div>
                    <span style={{ fontSize: 11, opacity: 0.75 }}>{onlineExams.length} Scheduled Items</span>
         </button>

         <button
          onClick={() => {
           handleExportAllExamsJSON();
           setIsExportHubModalOpen(false);
          }}
                    style={{
           padding: '12px 14px',
           borderRadius: '12px',
           background: 'rgba(255, 183, 77, 0.2)',
           border: '1px solid rgba(255, 183, 77, 0.45)',
           color: '#ffb74d',
           fontWeight: 800,
           fontSize: 12.5,
           cursor: 'pointer',
           display: 'flex',
           flexDirection: 'column',
           alignItems: 'center',
           gap: 4
          }}
         >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
           <Layers size={14} />
           <span>Both Exam Schedules</span>
          </div>
                    <span style={{ fontSize: 11, opacity: 0.75 }}>Offline + Online</span>
         </button>
        </div>
       </div>
      </div>

      {/* Modal Footer */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 }}>
       <button
        onClick={() => {
         setIsExportHubModalOpen(false);
         fileInputRef.current?.click();
        }}
                style={{
         padding: '10px 16px',
         borderRadius: 10,
         background: 'rgba(129, 199, 132, 0.16)',
         border: '1px solid rgba(129, 199, 132, 0.35)',
         color: '#81c784',
         fontWeight: 800,
         fontSize: 12.5,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <Upload size={14} />
        <span>Upload & Import JSON Instead</span>
       </button>

       <button
        onClick={() => setIsExportHubModalOpen(false)}
                style={{ padding: '10px 18px', borderRadius: 10, background: 'rgba(255,255,255,0.08)', border: '1px solid rgba(255,255,255,0.2)', color: '#fff', cursor: 'pointer', fontWeight: 700 }}
       >
        Close
       </button>
      </div>
     </div>
    </div>
   )}


   {/* Individual Day Quick Editor Modal */}
   {editingDayData && selectedStudentForDetail && (
        <div style={{
     position: 'fixed',
     top: 0,
     left: 0,
     right: 0,
     bottom: 0,
     background: 'rgba(0,0,0,0.85)',
     backdropFilter: 'blur(8px)',
     display: 'flex',
     alignItems: 'center',
     justifyContent: 'center',
     zIndex: 10001,
     padding: 16
    }}>
          <div style={{
      background: '#1a1829',
      border: '1px solid rgba(255,255,255,0.15)',
      borderRadius: 20,
      padding: 24,
      width: '100%',
      maxWidth: 540,
      display: 'flex',
      flexDirection: 'column',
      gap: 14,
      boxShadow: '0 20px 60px rgba(0,0,0,0.8)'
     }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ margin: 0, color: '#fff', fontSize: 18, fontWeight: 900, display: 'flex', alignItems: 'center', gap: 8 }}>
        <Edit2 size={18} color="#00e5ff" />
        <span>Customize Day {editingDayData.dayNumber} for {selectedStudentForDetail.name}</span>
       </h3>
       <button
        onClick={() => setEditingDayData(null)}
                style={{ background: 'transparent', border: 'none', color: 'rgba(255,255,255,0.6)', cursor: 'pointer' }}
       >
        <X size={20} />
       </button>
      </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
       <div>
                <label style={{ fontSize: 11, fontWeight: 700, color: 'rgba(255,255,255,0.6)', display: 'block', marginBottom: 4 }}>
         Date
        </label>
        <input
         type="text"
         value={editingDayData.date}
         onChange={e => setEditingDayData({ ...editingDayData, date: e.target.value })}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: 8, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', fontSize: 13 }}
        />
       </div>
       <div>
                <label style={{ fontSize: 11, fontWeight: 700, color: 'rgba(255,255,255,0.6)', display: 'block', marginBottom: 4 }}>
         Day Name
        </label>
        <input
         type="text"
         value={editingDayData.dayName}
         onChange={e => setEditingDayData({ ...editingDayData, dayName: e.target.value })}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: 8, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', fontSize: 13 }}
        />
       </div>
      </div>

      <div>
              <label style={{ fontSize: 11, fontWeight: 700, color: 'rgba(255,255,255,0.6)', display: 'block', marginBottom: 4 }}>
        Class Subject
       </label>
       <input
        type="text"
        value={editingDayData.classSubject}
        onChange={e => setEditingDayData({ ...editingDayData, classSubject: e.target.value })}
        placeholder="e.g. Physics (P-01) - Kinematics Mastery"
                style={{ width: '100%', padding: '9px 12px', borderRadius: 8, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', fontSize: 13 }}
       />
      </div>

      <div>
              <label style={{ fontSize: 11, fontWeight: 700, color: 'rgba(255,255,255,0.6)', display: 'block', marginBottom: 4 }}>
        Exam Name (Base)
       </label>
       <input
        type="text"
        value={editingDayData.examDetails}
        onChange={e => setEditingDayData({ ...editingDayData, examDetails: e.target.value })}
        placeholder="e.g. Engg. Weekly Exam-01"
                style={{ width: '100%', padding: '9px 12px', borderRadius: 8, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', fontSize: 13, marginBottom: 6 }}
       />
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        <input
         type="number"
         placeholder="MCQ Marks (e.g. 120)"
         onChange={e => setEditingDayData({ ...editingDayData, tempMcq: e.target.value })}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: 8, background: 'rgba(0, 229, 255, 0.05)', border: '1px solid rgba(0, 229, 255, 0.3)', color: '#fff', fontSize: 12 }}
        />
        <input
         type="number"
         placeholder="Written Marks (e.g. 180)"
         onChange={e => setEditingDayData({ ...editingDayData, tempWritten: e.target.value })}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: 8, background: 'rgba(255, 183, 77, 0.05)', border: '1px solid rgba(255, 183, 77, 0.3)', color: '#fff', fontSize: 12 }}
        />
       </div>
      </div>

      <div>
              <label style={{ fontSize: 11, fontWeight: 700, color: 'rgba(255,255,255,0.6)', display: 'block', marginBottom: 4 }}>
        Topics / Chapters Breakdown
       </label>
       <textarea
        value={editingDayData.topics}
        onChange={e => setEditingDayData({ ...editingDayData, topics: e.target.value })}
        placeholder="Comma separated topics"
        rows={3}
                style={{ width: '100%', padding: '9px 12px', borderRadius: 8, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)', color: '#fff', fontSize: 13, resize: 'vertical' }}
       />
      </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 8 }}>
       <button
        onClick={() => setEditingDayData(null)}
                style={{ padding: '10px 16px', borderRadius: 10, background: 'rgba(255,255,255,0.08)', border: 'none', color: '#fff', fontWeight: 700, cursor: 'pointer' }}
       >
        Cancel
       </button>
       <button
        onClick={() => {
         const isOnline = studentDetailSubTab === 'onlineRoutine';
         const baseList = isOnline
          ? ((studentCustomOnlineRoutine && studentCustomOnlineRoutine.length > 0) ? [...studentCustomOnlineRoutine] : [...onlineRoutines])
          : ((studentCustomRoutine && studentCustomRoutine.length > 0) ? [...studentCustomRoutine] : [...offlineRoutines]);
         const dayIdx = editingDayIndex !== null ? editingDayIndex : 0;
         
         let finalExamStr = editingDayData.examDetails.trim();
         if (editingDayData.tempMcq && editingDayData.tempWritten) {
          finalExamStr += ` MCQ (${editingDayData.tempMcq}) + Written (${editingDayData.tempWritten})`;
         } else if (editingDayData.tempMcq) {
          finalExamStr += ` MCQ (${editingDayData.tempMcq})`;
         } else if (editingDayData.tempWritten) {
          finalExamStr += ` Written (${editingDayData.tempWritten})`;
         }

         const updatedItem = {
          ...baseList[dayIdx],
          date: editingDayData.date,
          day: editingDayData.dayName,
          classSubject: editingDayData.classSubject,
          examDetails: finalExamStr || undefined,
          topics: editingDayData.topics.split(',').map((s: string) => s.trim()).filter(Boolean)
         };
         baseList[dayIdx] = updatedItem;
         handleSaveStudentDayEdit(
          selectedStudentForDetail.studentId,
          baseList,
          `Day ${editingDayData.dayNumber} updated: ${editingDayData.classSubject} (${editingDayData.date})`,
          isOnline ? 'Online' : 'Offline'
         );
        }}
        disabled={isSyncing}
                style={{ padding: '10px 18px', borderRadius: 10, background: '#00e5ff', border: 'none', color: '#000', fontWeight: 800, cursor: 'pointer' }}
       >
        {isSyncing ? 'Saving...' : 'Save & Push to Student'}
       </button>
      </div>
     </div>
    </div>
   )}

   {/* Demo JSON Code Drawer / Modal */}
   {isShowDemoJsonModal && (
        <div style={{
     position: 'fixed',
     top: 0,
     left: 0,
     right: 0,
     bottom: 0,
     background: 'rgba(0,0,0,0.85)',
     backdropFilter: 'blur(8px)',
     display: 'flex',
     alignItems: 'center',
     justifyContent: 'center',
     zIndex: 10002,
     padding: 16
    }}>
          <div style={{
      background: '#12111c',
      border: '1px solid rgba(255,255,255,0.15)',
      borderRadius: 20,
      padding: 24,
      width: '100%',
      maxWidth: 640,
      display: 'flex',
      flexDirection: 'column',
      gap: 14,
      boxShadow: '0 20px 60px rgba(0,0,0,0.8)'
     }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
       <div>
                <h3 style={{ margin: 0, color: '#ffd54f', fontSize: 18, fontWeight: 900, display: 'flex', alignItems: 'center', gap: 8 }}>
         <FileText size={18} color="#ffd54f" />
         <span>Demo JSON Code Template</span>
        </h3>
                <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.5)' }}>
         Use this validated JSON format to write or upload custom schedules for any student.
        </span>
       </div>
       <button
        onClick={() => setIsShowDemoJsonModal(false)}
                style={{ background: 'transparent', border: 'none', color: 'rgba(255,255,255,0.6)', cursor: 'pointer' }}
       >
        <X size={20} />
       </button>
      </div>

            <pre style={{
       background: '#090810',
       padding: 16,
       borderRadius: 12,
       border: '1px solid rgba(255,255,255,0.08)',
       color: '#00e5ff',
       fontFamily: 'monospace',
       fontSize: 12,
       lineHeight: 1.5,
       maxHeight: 320,
       overflowY: 'auto',
       margin: 0
      }}>
{`[
 {
  "dayNumber": 1,
  "date": "2026-08-20",
  "dayName": "Thu",
  "classSubject": "Physics (P-01) - Advanced Mechanics",
  "classRoom": "Room 402",
  "classTime": "10:00 AM - 12:30 PM",
  "examDetails": "Introductory MCQ (25 Marks)",
  "isExamDay": true,
  "examType": "Daily MCQ",
  "topics": "Kinematics, Projectile Motion, Newton's Laws & Friction",
  "tags": ["Physics", "Mechanics"]
 },
 {
  "dayNumber": 2,
  "date": "2026-08-21",
  "dayName": "Fri",
  "classSubject": "Chemistry (C-01) - Chemical Bonding",
  "classRoom": "Room 301",
  "classTime": "08:00 AM - 10:30 AM",
  "examDetails": "Bonding CQ + MCQ Practice Test",
  "isExamDay": true,
  "examType": "Paper-1 (50 Marks)",
  "topics": "Hybridization, Molecular Orbital Theory, Hydrogen Bonds",
  "tags": ["Chemistry", "Inorganic"]
 }
]`}
      </pre>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
       <button
        onClick={() => {
         const demoCode = `[\n {\n  "dayNumber": 1,\n  "date": "2026-08-20",\n  "dayName": "Thu",\n  "classSubject": "Physics (P-01) - Advanced Mechanics",\n  "classRoom": "Room 402",\n  "classTime": "10:00 AM - 12:30 PM",\n  "examDetails": "Introductory MCQ (25 Marks)",\n  "isExamDay": true,\n  "examType": "Daily MCQ",\n  "topics": "Kinematics, Projectile Motion, Newton's Laws & Friction",\n  "tags": ["Physics", "Mechanics"]\n },\n {\n  "dayNumber": 2,\n  "date": "2026-08-21",\n  "dayName": "Fri",\n  "classSubject": "Chemistry (C-01) - Chemical Bonding",\n  "classRoom": "Room 301",\n  "classTime": "08:00 AM - 10:30 AM",\n  "examDetails": "Bonding CQ + MCQ Practice Test",\n  "isExamDay": true,\n  "examType": "Paper-1 (50 Marks)",\n  "topics": "Hybridization, Molecular Orbital Theory, Hydrogen Bonds",\n  "tags": ["Chemistry", "Inorganic"]\n }\n]`;
         navigator.clipboard.writeText(demoCode);
         showNotification('Demo JSON copied to clipboard!');
        }}
                style={{
         padding: '10px 16px',
         borderRadius: 10,
         background: 'rgba(255, 213, 79, 0.2)',
         border: '1px solid rgba(255, 213, 79, 0.4)',
         color: '#ffd54f',
         fontWeight: 800,
         fontSize: 12.5,
         cursor: 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 6
        }}
       >
        <Copy size={13} />
        <span>Copy Demo JSON</span>
       </button>

       <button
        onClick={() => {
         const demoCode = `[\n {\n  "dayNumber": 1,\n  "date": "2026-08-20",\n  "dayName": "Thu",\n  "classSubject": "Physics (P-01) - Advanced Mechanics",\n  "classRoom": "Room 402",\n  "classTime": "10:00 AM - 12:30 PM",\n  "examDetails": "Introductory MCQ (25 Marks)",\n  "isExamDay": true,\n  "examType": "Daily MCQ",\n  "topics": "Kinematics, Projectile Motion, Newton's Laws & Friction",\n  "tags": ["Physics", "Mechanics"]\n },\n {\n  "dayNumber": 2,\n  "date": "2026-08-21",\n  "dayName": "Fri",\n  "classSubject": "Chemistry (C-01) - Chemical Bonding",\n  "classRoom": "Room 301",\n  "classTime": "08:00 AM - 10:30 AM",\n  "examDetails": "Bonding CQ + MCQ Practice Test",\n  "isExamDay": true,\n  "examType": "Paper-1 (50 Marks)",\n  "topics": "Hybridization, Molecular Orbital Theory, Hydrogen Bonds",\n  "tags": ["Chemistry", "Inorganic"]\n }\n]`;
         setDirectJsonInputText(demoCode);
         setIsShowDemoJsonModal(false);
         setIsDirectJsonEditorOpen(true);
        }}
                style={{
         padding: '10px 16px',
         borderRadius: 10,
         background: '#00e5ff',
         border: 'none',
         color: '#000',
         fontWeight: 800,
         fontSize: 12.5,
         cursor: 'pointer'
        }}
       >
        Load into Editor
       </button>
      </div>
     </div>
    </div>
   )}

   {/* Direct JSON Code Editor Modal */}
   {isDirectJsonEditorOpen && selectedStudentForDetail && (
        <div style={{
     position: 'fixed',
     top: 0,
     left: 0,
     right: 0,
     bottom: 0,
     background: 'rgba(0,0,0,0.85)',
     backdropFilter: 'blur(8px)',
     display: 'flex',
     alignItems: 'center',
     justifyContent: 'center',
     zIndex: 10002,
     padding: 16
    }}>
          <div style={{
      background: '#151421',
      border: '1px solid rgba(255,255,255,0.15)',
      borderRadius: 20,
      padding: 24,
      width: '100%',
      maxWidth: 680,
      display: 'flex',
      flexDirection: 'column',
      gap: 14,
      boxShadow: '0 20px 60px rgba(0,0,0,0.8)'
     }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
       <div>
                <h3 style={{ margin: 0, color: '#81c784', fontSize: 18, fontWeight: 900, display: 'flex', alignItems: 'center', gap: 8 }}>
         <Code2 size={18} color="#81c784" />
         <span>Direct Custom JSON Editor ({selectedStudentForDetail.name})</span>
        </h3>
                <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.5)' }}>
         Paste or modify JSON array of routine days directly to assign to this student.
        </span>
       </div>
       <button
        onClick={() => setIsDirectJsonEditorOpen(false)}
                style={{ background: 'transparent', border: 'none', color: 'rgba(255,255,255,0.6)', cursor: 'pointer' }}
       >
        <X size={20} />
       </button>
      </div>

      <textarea
       value={directJsonInputText}
       onChange={e => setDirectJsonInputText(e.target.value)}
       placeholder="Paste JSON array here..."
       rows={12}
              style={{
        width: '100%',
        padding: 14,
        borderRadius: 12,
        background: '#090810',
        border: '1px solid rgba(255,255,255,0.12)',
        color: '#81c784',
        fontFamily: 'monospace',
        fontSize: 12,
        lineHeight: 1.4,
        resize: 'vertical'
       }}
      />

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
       <button
        onClick={() => setIsDirectJsonEditorOpen(false)}
                style={{ padding: '10px 16px', borderRadius: 10, background: 'rgba(255,255,255,0.08)', border: 'none', color: '#fff', fontWeight: 700, cursor: 'pointer' }}
       >
        Cancel
       </button>
       <button
        onClick={() => handleApplyDirectJsonToStudent(selectedStudentForDetail.studentId, directJsonInputText)}
        disabled={isSyncing}
                style={{
         padding: '10px 20px',
         borderRadius: 10,
         background: '#81c784',
         border: 'none',
         color: '#000',
         fontWeight: 900,
         fontSize: 13,
         cursor: 'pointer'
        }}
       >
        {isSyncing ? 'Deploying...' : 'Deploy JSON to Student'}
       </button>
      </div>
     </div>
    </div>
   )}

   {/* ========================================================================= */}
   {/* GLOBAL CUSTOM CONFIRMATION MODAL (REPLACES NATIVE BROWSER POPUPS)    */}
   {/* ========================================================================= */}
   {confirmDialog && confirmDialog.isOpen && (
        <div style={{
     position: 'fixed',
     inset: 0,
     background: 'rgba(0, 0, 0, 0.78)',
     backdropFilter: 'blur(12px)',
     WebkitBackdropFilter: 'blur(12px)',
     zIndex: 10000,
     display: 'flex',
     alignItems: 'center',
     justifyContent: 'center',
     padding: 20,
     animation: 'fadeIn 0.15s ease-out'
    }}>
     <div
      className="haze-card"
            style={{
       maxWidth: 440,
       width: '100%',
       borderRadius: '24px',
       padding: '26px 24px',
       display: 'flex',
       flexDirection: 'column',
       gap: 16,
       background: 'linear-gradient(135deg, rgba(29, 27, 32, 0.98) 0%, rgba(38, 35, 45, 0.95) 100%)',
       border: confirmDialog.isDanger ? '1px solid rgba(239, 83, 80, 0.45)' : '1px solid rgba(0, 229, 255, 0.45)',
       boxShadow: confirmDialog.isDanger ? '0 20px 60px rgba(239, 83, 80, 0.25)' : '0 20px 60px rgba(0, 229, 255, 0.25)'
      }}
     >
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <div style={{
        width: 44,
        height: 44,
        borderRadius: 14,
        background: confirmDialog.isDanger ? 'rgba(239, 83, 80, 0.16)' : 'rgba(0, 229, 255, 0.16)',
        border: confirmDialog.isDanger ? '1px solid rgba(239, 83, 80, 0.35)' : '1px solid rgba(0, 229, 255, 0.35)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: confirmDialog.isDanger ? '#ef5350' : '#00e5ff'
       }}>
        {confirmDialog.isDanger ? <Trash2 size={22} /> : <RotateCcw size={22} />}
       </div>
              <div style={{ flex: 1 }}>
                <h3 style={{ margin: 0, fontSize: 17, fontWeight: 900, color: '#fff', letterSpacing: '0.2px' }}>
         {confirmDialog.title}
        </h3>
       </div>
       <button
        onClick={() => setConfirmDialog(null)}
                style={{ background: 'transparent', border: 'none', color: 'rgba(255,255,255,0.5)', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
       >
        <X size={20} />
       </button>
      </div>

            <p style={{ margin: 0, fontSize: 13.5, color: 'rgba(255, 255, 255, 0.75)', lineHeight: 1.55 }}>
       {confirmDialog.message}
      </p>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 8 }}>
       <button
        onClick={() => setConfirmDialog(null)}
                style={{
         padding: '10px 18px',
         borderRadius: '12px',
         background: 'rgba(255, 255, 255, 0.08)',
         border: '1px solid rgba(255, 255, 255, 0.15)',
         color: '#fff',
         fontWeight: 700,
         fontSize: 13,
         cursor: 'pointer'
        }}
       >
        {confirmDialog.cancelText || 'Cancel'}
       </button>
       <button
        onClick={confirmDialog.onConfirm}
                style={{
         padding: '10px 22px',
         borderRadius: '12px',
         background: confirmDialog.isDanger
          ? 'linear-gradient(135deg, #ef5350, #d32f2f)'
          : 'linear-gradient(135deg, #00e5ff, #00b0ff)',
         border: 'none',
         color: confirmDialog.isDanger ? '#fff' : '#000',
         fontWeight: 900,
         fontSize: 13,
         cursor: 'pointer',
         boxShadow: confirmDialog.isDanger ? '0 4px 14px rgba(239, 83, 80, 0.4)' : '0 4px 14px rgba(0, 229, 255, 0.4)'
        }}
       >
        {confirmDialog.confirmText || 'Confirm'}
       </button>
      </div>
     </div>
    </div>
   )}

   {/* CUSTOM GLASSMORPHIC TEXT INPUT PROMPT MODAL */}
   {inputModalDialog && inputModalDialog.isOpen && (
        <div style={{
     position: 'fixed',
     top: 0,
     left: 0,
     right: 0,
     bottom: 0,
     background: 'rgba(0, 0, 0, 0.75)',
     backdropFilter: 'blur(12px)',
     WebkitBackdropFilter: 'blur(12px)',
     zIndex: 10001,
     display: 'flex',
     alignItems: 'center',
     justifyContent: 'center',
     padding: 20
    }}>
     <form
      onSubmit={(e) => {
       e.preventDefault();
       if (inputModalValue.trim()) {
        const cb = inputModalDialog.onConfirm;
        const val = inputModalValue.trim();
        setInputModalDialog(null);
        cb(val);
       }
      }}
      className="haze-card"
            style={{
       maxWidth: 460,
       width: '100%',
       borderRadius: '24px',
       padding: '26px 24px',
       display: 'flex',
       flexDirection: 'column',
       gap: 16,
       background: 'linear-gradient(135deg, rgba(29, 27, 32, 0.98) 0%, rgba(38, 35, 45, 0.95) 100%)',
       border: '1px solid rgba(208, 188, 255, 0.4)',
       boxShadow: '0 20px 60px rgba(0, 0, 0, 0.6)'
      }}
     >
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <div style={{
        width: 44,
        height: 44,
        borderRadius: 14,
        background: 'rgba(208, 188, 255, 0.16)',
        border: '1px solid rgba(208, 188, 255, 0.35)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: '#d0bcff'
       }}>
        <Edit2 size={20} />
       </div>
              <div style={{ flex: 1 }}>
                <h3 style={{ margin: 0, fontSize: 17, fontWeight: 900, color: '#fff', letterSpacing: '0.2px' }}>
         {inputModalDialog.title}
        </h3>
        {inputModalDialog.subtitle && (
                  <p style={{ margin: '3px 0 0', fontSize: 12, color: 'rgba(255, 255, 255, 0.65)' }}>
          {inputModalDialog.subtitle}
         </p>
        )}
       </div>
       <button
        type="button"
        onClick={() => setInputModalDialog(null)}
                style={{ background: 'transparent', border: 'none', color: 'rgba(255,255,255,0.5)', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
       >
        <X size={20} />
       </button>
      </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
       <input
        type="text"
        autoFocus
        value={inputModalValue}
        onChange={(e) => setInputModalValue(e.target.value)}
        placeholder={inputModalDialog.placeholder || 'Type here...'}
                style={{
         width: '100%',
         padding: '12px 14px',
         borderRadius: '12px',
         background: 'rgba(255, 255, 255, 0.08)',
         border: '1px solid rgba(208, 188, 255, 0.4)',
         color: '#fff',
         fontSize: 14,
         fontWeight: 700,
         outline: 'none'
        }}
       />
      </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 4 }}>
       <button
        type="button"
        onClick={() => setInputModalDialog(null)}
                style={{
         padding: '10px 18px',
         borderRadius: '12px',
         background: 'rgba(255, 255, 255, 0.08)',
         border: '1px solid rgba(255, 255, 255, 0.15)',
         color: '#fff',
         fontWeight: 700,
         fontSize: 13,
         cursor: 'pointer'
        }}
       >
        Cancel
       </button>
       <button
        type="submit"
        disabled={!inputModalValue.trim()}
                style={{
         padding: '10px 22px',
         borderRadius: '12px',
         background: 'linear-gradient(135deg, #d0bcff, #b69df8)',
         border: 'none',
         color: '#381e72',
         fontWeight: 900,
         fontSize: 13,
         cursor: inputModalValue.trim() ? 'pointer' : 'default',
         boxShadow: '0 4px 14px rgba(208, 188, 255, 0.35)'
        }}
       >
        {inputModalDialog.confirmText || 'Save'}
       </button>
      </div>
     </form>
    </div>
   )}

   {/* MASS ASSIGN PRESET MODAL */}
   {isMassAssignModalOpen && (
        <div style={{
     position: 'fixed',
     top: 0,
     left: 0,
     right: 0,
     bottom: 0,
     background: 'rgba(0, 0, 0, 0.8)',
     backdropFilter: 'blur(10px)',
     WebkitBackdropFilter: 'blur(10px)',
     zIndex: 9999,
     display: 'flex',
     alignItems: 'center',
     justifyContent: 'center',
     padding: 20
    }}>
     <div
      className="haze-card"
            style={{
       maxWidth: 580,
       width: '100%',
       borderRadius: '24px',
       padding: '24px',
       display: 'flex',
       flexDirection: 'column',
       gap: 16,
       background: 'linear-gradient(135deg, rgba(29, 27, 32, 0.98) 0%, rgba(38, 35, 45, 0.95) 100%)',
       border: '1px solid rgba(208, 188, 255, 0.35)',
       boxShadow: '0 20px 60px rgba(0, 0, 0, 0.6)'
      }}
     >
      {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <div style={{ width: 40, height: 40, borderRadius: 12, background: 'rgba(208, 188, 255, 0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#d0bcff' }}>
         <Send size={20} />
        </div>
        <div>
                  <h3 style={{ margin: 0, fontSize: 16.5, fontWeight: 900, color: '#fff' }}>
          Deploy Track / Preset to Candidates
         </h3>
                  <p style={{ margin: 0, fontSize: 12, color: 'rgba(255, 255, 255, 0.65)' }}>
          Deploy customized syllabus & routines to {selectedStudentRosterIds.length > 0 ? `${selectedStudentRosterIds.length} candidate(s)` : 'candidates'}.
         </p>
        </div>
       </div>

       <button
        onClick={() => setIsMassAssignModalOpen(false)}
                style={{ background: 'transparent', border: 'none', color: 'rgba(255,255,255,0.6)', cursor: 'pointer', padding: 4 }}
       >
        <X size={20} />
       </button>
      </div>

      {/* Choose Preset */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <label style={{ fontSize: 12, fontWeight: 800, color: '#d0bcff' }}>
        Select Preset / Track Template:
       </label>
       <select
        value={selectedPresetForMassAssign ? selectedPresetForMassAssign.id : (presetsList[0]?.id || '')}
        onChange={(e) => {
         const p = presetsList.find(item => item.id === e.target.value);
         if (p) setSelectedPresetForMassAssign(p);
        }}
                style={{
         padding: '12px 14px',
         borderRadius: '12px',
         background: 'rgba(255, 255, 255, 0.08)',
         border: '1px solid rgba(208, 188, 255, 0.35)',
         color: '#fff',
         fontSize: 13.5,
         fontWeight: 800
        }}
       >
        {presetsList.map((preset) => (
                  <option key={preset.id} value={preset.id} style={{ background: '#1d1b20' }}>
          [{preset.category}] {preset.name}
         </option>
        ))}
       </select>
      </div>

      {/* Target Students Preview */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <label style={{ fontSize: 12, fontWeight: 800, color: '#00e5ff' }}>
         Target Candidates ({selectedStudentRosterIds.length > 0 ? selectedStudentRosterIds.length : 'Choose below'}):
        </label>
        {selectedStudentRosterIds.length === 0 && (
                  <span style={{ fontSize: 11, color: '#ffd54f' }}>
          (Pick candidates from the list)
         </span>
        )}
       </div>

       {selectedStudentRosterIds.length > 0 ? (
                <div style={{
         display: 'flex',
         flexWrap: 'wrap',
         gap: 6,
         maxHeight: 120,
         overflowY: 'auto',
         background: 'rgba(0, 0, 0, 0.25)',
         padding: '10px 12px',
         borderRadius: '12px',
         border: '1px solid rgba(255, 255, 255, 0.08)'
        }}>
         {selectedStudentRosterIds.map((sid) => {
          const st = studentsList.find(s => s.studentId === sid);
          return (
           <span
            key={sid}
                        style={{
             fontSize: 11,
             fontWeight: 700,
             background: 'rgba(208, 188, 255, 0.15)',
             border: '1px solid rgba(208, 188, 255, 0.3)',
             color: '#d0bcff',
             padding: '3px 8px',
             borderRadius: 6
            }}
           >
            {sid} ({st?.name || 'Aspirant'})
           </span>
          );
         })}
        </div>
       ) : (
        <select
         multiple
         value={selectedStudentRosterIds}
         onChange={(e) => {
          const opts = Array.from(e.target.selectedOptions).map(o => o.value);
          setSelectedStudentRosterIds(opts);
         }}
                  style={{
          padding: '8px 10px',
          borderRadius: '12px',
          background: 'rgba(255, 255, 255, 0.06)',
          border: '1px solid rgba(255, 255, 255, 0.15)',
          color: '#fff',
          fontSize: 12,
          height: 100
         }}
        >
         {studentsList.map((s, idx) => (
                    <option key={idx} value={s.studentId} style={{ background: '#1d1b20', padding: '4px 6px' }}>
           {s.studentId} — {s.name} ({s.college || 'Engineering Aspirant'})
          </option>
         ))}
        </select>
       )}
      </div>

      {/* Deploy Action */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 8 }}>
       <button
        onClick={() => setIsMassAssignModalOpen(false)}
                style={{
         padding: '10px 18px',
         borderRadius: '12px',
         background: 'rgba(255, 255, 255, 0.08)',
         border: '1px solid rgba(255, 255, 255, 0.15)',
         color: '#fff',
         fontWeight: 700,
         fontSize: 13,
         cursor: 'pointer'
        }}
       >
        Cancel
       </button>

       <button
        onClick={() => {
         const p = selectedPresetForMassAssign || presetsList[0];
         if (p) handleMassApplyPreset(p, selectedStudentRosterIds);
        }}
        disabled={isSyncing || selectedStudentRosterIds.length === 0}
                style={{
         padding: '10px 22px',
         borderRadius: '12px',
         background: 'linear-gradient(135deg, #d0bcff, #b69df8)',
         border: 'none',
         color: '#381e72',
         fontWeight: 900,
         fontSize: 13.5,
         cursor: (isSyncing || selectedStudentRosterIds.length === 0) ? 'not-allowed' : 'pointer',
         display: 'flex',
         alignItems: 'center',
         gap: 8,
         boxShadow: '0 4px 14px rgba(208, 188, 255, 0.35)'
        }}
       >
        <Zap size={16} />
        <span>{isSyncing ? 'Deploying...' : `Deploy to ${selectedStudentRosterIds.length} Candidate(s) `}</span>
       </button>
      </div>
     </div>
    </div>
   )}

   {/* Change Master Password & Security Modal */}
   {isChangePasswordModalOpen && (
     <div
       style={{
         position: 'fixed',
         inset: 0,
         background: 'rgba(0,0,0,0.85)',
         zIndex: 9999,
         display: 'flex',
         alignItems: 'center',
         justifyContent: 'center',
         padding: 20,
         backdropFilter: 'blur(14px)'
       }}
     >
       <div
         className="haze-card"
         style={{
           maxWidth: 480,
           width: '100%',
           maxHeight: '90vh',
           overflowY: 'auto',
           borderRadius: 24,
           padding: 26,
           display: 'flex',
           flexDirection: 'column',
           gap: 20,
           boxShadow: '0 24px 64px rgba(0,0,0,0.6)'
         }}
       >
         {/* Modal Header */}
         <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
           <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
             <div
               style={{
                 width: 40,
                 height: 40,
                 borderRadius: 12,
                 background: 'rgba(255, 213, 79, 0.2)',
                 display: 'flex',
                 alignItems: 'center',
                 justifyContent: 'center',
                 color: '#ffd54f'
               }}
             >
               <Key size={20} />
             </div>
             <div>
               <h3 style={{ margin: 0, color: '#fff', fontSize: 17, fontWeight: 900 }}>
                 Admin Security & Access Control
               </h3>
               <span style={{ fontSize: 12, color: 'rgba(255,255,255,0.6)' }}>
                 Manage Master Password & Authorized Google Admin Accounts
               </span>
             </div>
           </div>
           <button
             onClick={() => setIsChangePasswordModalOpen(false)}
             style={{ background: 'transparent', border: 'none', color: 'rgba(255,255,255,0.6)', cursor: 'pointer' }}
           >
             <X size={20} />
           </button>
         </div>

         {/* Section 1: Authorized Google Admin Emails Whitelist */}
         <div style={{ display: 'flex', flexDirection: 'column', gap: 10, padding: 14, borderRadius: 16, background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.1)' }}>
           <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
             <Users size={16} color="#d0bcff" />
             <span style={{ fontSize: 13, fontWeight: 800, color: '#fff' }}>Authorized Google Admins (Whitelist)</span>
           </div>
           <p style={{ margin: 0, fontSize: 11.5, color: 'rgba(255,255,255,0.6)' }}>
             Only Google accounts listed here can sign in. All other Gmail accounts are strictly blocked.
           </p>

           {/* List of Whitelisted Emails */}
           <div style={{ display: 'flex', flexDirection: 'column', gap: 6, maxHeight: 130, overflowY: 'auto' }}>
             {authorizedEmailsList.length === 0 ? (
               <span style={{ fontSize: 12, color: '#ffd54f', fontStyle: 'italic' }}>
                 No whitelist set yet. The first Google account to sign in will automatically become SuperAdmin.
               </span>
             ) : (
               authorizedEmailsList.map((email, idx) => (
                 <div
                   key={idx}
                   style={{
                     display: 'flex',
                     alignItems: 'center',
                     justifyContent: 'space-between',
                     padding: '6px 12px',
                     borderRadius: 10,
                     background: 'rgba(208, 188, 255, 0.08)',
                     border: '1px solid rgba(208, 188, 255, 0.2)',
                     fontSize: 12,
                     color: '#fff'
                   }}
                 >
                   <span>{email}</span>
                   <button
                     type="button"
                     onClick={() => {
                       firebaseAuthService.removeAuthorizedEmail(email);
                       setAuthorizedEmailsList(firebaseAuthService.getAuthorizedEmails());
                       showNotification(`Removed "${email}" from Admin Whitelist`);
                     }}
                     title="Remove Admin"
                     style={{ background: 'transparent', border: 'none', color: '#ef5350', cursor: 'pointer', display: 'flex', alignItems: 'center' }}
                   >
                     <Trash2 size={13} />
                   </button>
                 </div>
               ))
             )}
           </div>

           {/* Add New Admin Email */}
           <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
             <input
               type="email"
               placeholder="admin@gmail.com"
               value={newAdminEmailInput}
               onChange={(e) => setNewAdminEmailInput(e.target.value)}
               style={{ flex: 1, padding: '8px 12px', borderRadius: 10, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.15)', color: '#fff', fontSize: 12 }}
             />
             <button
               type="button"
               disabled={!newAdminEmailInput.trim()}
               onClick={() => {
                 if (newAdminEmailInput.trim()) {
                   firebaseAuthService.addAuthorizedEmail(newAdminEmailInput.trim());
                   setAuthorizedEmailsList(firebaseAuthService.getAuthorizedEmails());
                   showNotification(`Added "${newAdminEmailInput.trim()}" as Authorized Admin`);
                   setNewAdminEmailInput('');
                 }
               }}
               style={{
                 padding: '8px 14px',
                 borderRadius: 10,
                 background: '#d0bcff',
                 color: '#381e72',
                 border: 'none',
                 fontWeight: 800,
                 fontSize: 12,
                 cursor: newAdminEmailInput.trim() ? 'pointer' : 'default'
               }}
             >
               Add Admin
             </button>
           </div>
         </div>

         {/* Section 2: Master Password */}
         <form onSubmit={handleChangePassword} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
           <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
             <Key size={16} color="#ffd54f" />
             <span style={{ fontSize: 13, fontWeight: 800, color: '#fff' }}>Change Master Admin Password</span>
           </div>

           <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
             <label style={{ fontSize: 11.5, fontWeight: 700, color: '#d0bcff' }}>Current Master Password</label>
             <input
               type="password"
               placeholder="Enter current password (default: admin)"
               value={currentPasswordInput}
               onChange={(e) => setCurrentPasswordInput(e.target.value)}
               style={{ width: '100%', padding: '10px 12px', borderRadius: 10, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.15)', color: '#fff', fontSize: 13 }}
             />
           </div>

           <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
             <label style={{ fontSize: 11.5, fontWeight: 700, color: '#d0bcff' }}>New Master Password</label>
             <input
               type="password"
               placeholder="At least 4 characters"
               value={newPasswordInput}
               onChange={(e) => setNewPasswordInput(e.target.value)}
               style={{ width: '100%', padding: '10px 12px', borderRadius: 10, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.15)', color: '#fff', fontSize: 13 }}
             />
           </div>

           <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
             <label style={{ fontSize: 11.5, fontWeight: 700, color: '#d0bcff' }}>Confirm New Password</label>
             <input
               type="password"
               placeholder="Re-type new password"
               value={confirmPasswordInput}
               onChange={(e) => setConfirmPasswordInput(e.target.value)}
               style={{ width: '100%', padding: '10px 12px', borderRadius: 10, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.15)', color: '#fff', fontSize: 13 }}
             />
           </div>

           {changePasswordError && (
             <div style={{ padding: '8px 12px', borderRadius: 10, background: 'rgba(239, 83, 80, 0.15)', border: '1px solid rgba(239, 83, 80, 0.3)', color: '#ef5350', fontSize: 12, fontWeight: 600 }}>
               {changePasswordError}
             </div>
           )}

           <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 4 }}>
             <button
               type="button"
               onClick={() => setIsChangePasswordModalOpen(false)}
               style={{ padding: '9px 16px', borderRadius: 10, background: 'rgba(255,255,255,0.08)', border: '1px solid rgba(255,255,255,0.15)', color: '#fff', cursor: 'pointer', fontWeight: 600, fontSize: 12.5 }}
             >
               Close
             </button>
             <button
               type="submit"
               disabled={!currentPasswordInput || !newPasswordInput || !confirmPasswordInput}
               style={{
                 padding: '9px 18px',
                 borderRadius: 10,
                 background: 'linear-gradient(135deg, #ffd54f, #ffb300)',
                 border: 'none',
                 color: '#3e2723',
                 fontWeight: 900,
                 cursor: (!currentPasswordInput || !newPasswordInput || !confirmPasswordInput) ? 'not-allowed' : 'pointer'
               }}
             >
               Save New Password
             </button>
           </div>
         </form>
       </div>
     </div>
   )}
  </div>
 );
};

// Modal for Routine Edit (with Calendar Date Picker)
const RoutineEditModal: React.FC<{
 initialItem?: RoutineItem;
 onSave: (item: RoutineItem) => void;
 onDismiss: () => void;
}> = ({ initialItem, onSave, onDismiss }) => {
 // Parse existing date string like "22-Aug-26" → "2026-08-22" for input
 const parseToInputDate = (dateStr: string): string => {
  if (!dateStr) return '';
  // If already in YYYY-MM-DD format
  if (/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) return dateStr;
  // Try parse "22-Aug-26" or "22-Aug-2026"
  const months: Record<string, string> = {
   Jan: '01', Feb: '02', Mar: '03', Apr: '04', May: '05', Jun: '06',
   Jul: '07', Aug: '08', Sep: '09', Oct: '10', Nov: '11', Dec: '12'
  };
  const m = dateStr.match(/^(\d{1,2})-([A-Za-z]{3})-(\d{2,4})$/);
  if (m) {
   const day = m[1].padStart(2, '0');
   const mon = months[m[2]] || '01';
   const yr = m[3].length === 2 ? `20${m[3]}` : m[3];
   return `${yr}-${mon}-${day}`;
  }
  return '';
 };

 // Format YYYY-MM-DD → "22-Aug-26"
 const formatDisplayDate = (isoDate: string): string => {
  if (!isoDate) return '';
  const d = new Date(isoDate + 'T00:00:00');
  if (isNaN(d.getTime())) return isoDate;
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  const dd = String(d.getDate()).padStart(2, '0');
  const mon = months[d.getMonth()];
  const yr = String(d.getFullYear()).slice(2);
  return `${dd}-${mon}-${yr}`;
 };

 const getDayName = (isoDate: string): string => {
  if (!isoDate) return '';
  const d = new Date(isoDate + 'T00:00:00');
  if (isNaN(d.getTime())) return '';
  return ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'][d.getDay()];
 };

 const [inputDate, setInputDate] = useState(parseToInputDate(initialItem?.date || ''));
 const [classSubject, setClassSubject] = useState(initialItem?.classSubject || '');
 const [examDetails, setExamDetails] = useState(initialItem?.examDetails || '');
 const [mcqMarks, setMcqMarks] = useState('');
 const [writtenMarks, setWrittenMarks] = useState('');
 const [topics, setTopics] = useState<string[]>(initialItem?.topics && initialItem.topics.length > 0 ? initialItem.topics : ['']);

 const displayDate = formatDisplayDate(inputDate);
 const dayName = getDayName(inputDate);

 return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.75)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 20 }}>
   <div
    className="haze-card"
        style={{
     maxWidth: 520,
     width: '100%',
     maxHeight: '90vh',
     overflowY: 'auto',
     borderRadius: 28,
     padding: 24,
     display: 'flex',
     flexDirection: 'column',
     gap: 16
    }}
   >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h3 style={{ margin: 0, color: '#fff', fontSize: 18, fontWeight: 900 }}>
      {initialItem ? 'Edit Routine Day' : 'Add Routine Day'}
     </h3>
          <button onClick={onDismiss} style={{ background: 'transparent', border: 'none', color: '#fff', cursor: 'pointer' }}>
      <X size={20} />
     </button>
    </div>

    {/* Calendar Date Picker */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <label style={{ fontSize: 11, fontWeight: 700, color: '#d0bcff' }}>Select Date (Calendar)</label>
          <div style={{ position: 'relative' }}>
      <input
       type="date"
       value={inputDate}
       onChange={(e) => setInputDate(e.target.value)}
              style={{
        width: '100%',
        padding: '12px 14px',
        borderRadius: 12,
        background: 'rgba(208, 188, 255, 0.1)',
        border: '1.5px solid rgba(208, 188, 255, 0.4)',
        color: '#fff',
        fontSize: 15,
        fontWeight: 700,
        colorScheme: 'dark',
        boxSizing: 'border-box'
       }}
      />
     </div>
     {inputDate && (
            <div style={{ display: 'flex', gap: 10, marginTop: 4 }}>
              <span style={{ padding: '4px 12px', borderRadius: 8, background: 'rgba(208,188,255,0.15)', color: '#d0bcff', fontSize: 12, fontWeight: 800 }}>
        {displayDate}
       </span>
              <span style={{ padding: '4px 12px', borderRadius: 8, background: 'rgba(255,255,255,0.08)', color: '#ccc', fontSize: 12, fontWeight: 700 }}>
        {dayName}
       </span>
      </div>
     )}
    </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <label style={{ fontSize: 11, fontWeight: 700, color: '#d0bcff' }}>Class Subject</label>
     <input
      type="text"
      placeholder="Physics (P-01)"
      value={classSubject}
      onChange={(e) => setClassSubject(e.target.value)}
            style={{ padding: 10, borderRadius: 10, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.2)', color: '#fff' }}
     />
    </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <label style={{ fontSize: 11, fontWeight: 700, color: '#d0bcff' }}>Exam Name (Base Name)</label>
     <input
      type="text"
      placeholder="e.g. Engg. Weekly Exam-01"
      value={examDetails}
      onChange={(e) => setExamDetails(e.target.value)}
            style={{ padding: 10, borderRadius: 10, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.2)', color: '#fff', marginBottom: 6 }}
     />
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
      <div>
              <label style={{ fontSize: 10, color: '#00e5ff', fontWeight: 600 }}>MCQ Marks</label>
       <input
        type="number"
        placeholder="e.g. 120"
        value={mcqMarks}
        onChange={(e) => setMcqMarks(e.target.value)}
                style={{ width: '100%', padding: 8, borderRadius: 8, background: 'rgba(0, 229, 255, 0.05)', border: '1px solid rgba(0, 229, 255, 0.3)', color: '#fff' }}
       />
      </div>
      <div>
              <label style={{ fontSize: 10, color: '#ffb74d', fontWeight: 600 }}>Written Marks</label>
       <input
        type="number"
        placeholder="e.g. 180"
        value={writtenMarks}
        onChange={(e) => setWrittenMarks(e.target.value)}
                style={{ width: '100%', padding: 8, borderRadius: 8, background: 'rgba(255, 183, 77, 0.05)', border: '1px solid rgba(255, 183, 77, 0.3)', color: '#fff' }}
       />
      </div>
     </div>
    </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <label style={{ fontSize: 11, fontWeight: 700, color: '#d0bcff' }}>Topics / Syllabus Parts</label>
      <button
       onClick={() => setTopics((prev) => [...prev, ''])}
              style={{ background: 'transparent', border: 'none', color: '#d0bcff', fontSize: 12, fontWeight: 800, cursor: 'pointer' }}
      >
       + Add Part
      </button>
     </div>

     {topics.map((t, idx) => (
            <div key={idx} style={{ display: 'flex', gap: 6 }}>
       <input
        type="text"
        placeholder={`Part-${String(idx + 1).padStart(2, '0')}: Topic details...`}
        value={t}
        onChange={(e) => {
         const val = e.target.value;
         setTopics((prev) => prev.map((item, i) => (i === idx ? val : item)));
        }}
                style={{ flex: 1, padding: 8, borderRadius: 8, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.2)', color: '#fff', fontSize: 13 }}
       />
       {topics.length > 1 && (
        <button
         onClick={() => setTopics((prev) => prev.filter((_, i) => i !== idx))}
                  style={{ background: 'rgba(239,83,80,0.15)', border: 'none', color: '#ef5350', borderRadius: 8, width: 32, cursor: 'pointer' }}
        >
         <Trash2 size={14} />
        </button>
       )}
      </div>
     ))}
    </div>

        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 8 }}>
          <button onClick={onDismiss} style={{ padding: '10px 18px', borderRadius: 10, background: 'transparent', border: '1px solid rgba(255,255,255,0.2)', color: '#fff', cursor: 'pointer' }}>
      Cancel
     </button>
     <button
      disabled={!inputDate}
      onClick={() => {
       if (!inputDate) return;
       const cleanTopics = topics.map((t) => t.trim()).filter(Boolean);
       
       let finalExamStr = examDetails.trim();
       if (mcqMarks && writtenMarks) {
        finalExamStr += ` MCQ (${mcqMarks}) + Written (${writtenMarks})`;
       } else if (mcqMarks) {
        finalExamStr += ` MCQ (${mcqMarks})`;
       } else if (writtenMarks) {
        finalExamStr += ` Written (${writtenMarks})`;
       }

       onSave({
        id: initialItem?.id || `rot-${Date.now()}`,
        date: displayDate,
        day: dayName,
        classSubject: classSubject.trim() || undefined,
        examDetails: finalExamStr || undefined,
        topics: cleanTopics
       });
      }}
            style={{ padding: '10px 20px', borderRadius: 10, background: inputDate ? '#d0bcff' : 'rgba(208,188,255,0.3)', color: '#381e72', border: 'none', fontWeight: 800, cursor: inputDate ? 'pointer' : 'default' }}
     >
      Save Routine Day
     </button>
    </div>
   </div>
  </div>
 );
};

// Modal for Exam Edit
const ExamEditModal: React.FC<{
 initialItem?: any;
 onSave: (item: any) => void;
 onDismiss: () => void;
}> = ({ initialItem, onSave, onDismiss }) => {

 // ── Date helpers (same as RoutineEditModal) ──────────────────
 const parseToInputDate = (dateStr: string): string => {
  if (!dateStr) return '';
  if (/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) return dateStr;
  const months: Record<string, string> = {
   Jan: '01', Feb: '02', Mar: '03', Apr: '04', May: '05', Jun: '06',
   Jul: '07', Aug: '08', Sep: '09', Oct: '10', Nov: '11', Dec: '12'
  };
  const m = dateStr.match(/^(\d{1,2})-([A-Za-z]{3})-(\d{2,4})$/);
  if (m) {
   const d = m[1].padStart(2, '0');
   const mo = months[m[2]] || '01';
   const yr = m[3].length === 2 ? `20${m[3]}` : m[3];
   return `${yr}-${mo}-${d}`;
  }
  return '';
 };

 const formatDisplayDate = (iso: string): string => {
  if (!iso) return '';
  const d = new Date(iso + 'T00:00:00');
  if (isNaN(d.getTime())) return iso;
  const ms = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  return `${String(d.getDate()).padStart(2,'0')}-${ms[d.getMonth()]}-${String(d.getFullYear()).slice(2)}`;
 };

 const getDayName = (iso: string): string => {
  if (!iso) return '';
  const d = new Date(iso + 'T00:00:00');
  if (isNaN(d.getTime())) return '';
  return ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'][d.getDay()];
 };
 // ─────────────────────────────────────────────────────────────

 const [inputDate, setInputDate] = useState(parseToInputDate(initialItem?.date || ''));
 
 // অটো-পার্সার: পুরনো টেক্সট থেকে মার্কস আলাদা করার লজিক
 const parseExamString = (examStr: string) => {
  let baseName = examStr;
  let mcq = "";
  let written = "";

  const mcqMatch = examStr.match(/MCQ\s*\(\s*(\d+)\s*\)/i);
  if (mcqMatch) {
   mcq = mcqMatch[1];
   baseName = baseName.replace(mcqMatch[0], "");
  }

  const writtenMatch = examStr.match(/(?:Written|Wri\.)\s*\(\s*(\d+)\s*\)/i);
  if (writtenMatch) {
   written = writtenMatch[1];
   baseName = baseName.replace(writtenMatch[0], "");
  }

  baseName = baseName.replace(/\+\s*$/, "").replace(/\s+$/, "").trim();
  return { baseName, mcq, written };
 };

 const [examsState, setExamsState] = useState<{baseName: string, mcq: string, written: string}[]>(
  initialItem?.exams && initialItem.exams.length > 0 
   ? initialItem.exams.map(parseExamString) 
   : [{ baseName: '', mcq: '', written: '' }]
 );

 const [syllabus, setSyllabus] = useState<string[]>(initialItem?.syllabus && initialItem.syllabus.length > 0 ? initialItem.syllabus : ['']);

 const displayDate = formatDisplayDate(inputDate);
 const dayName = getDayName(inputDate);

 return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 20 }}>
   <div
    className="haze-card"
        style={{
     maxWidth: 520,
     width: '100%',
     maxHeight: '90vh',
     overflowY: 'auto',
     borderRadius: 28,
     padding: 24,
     display: 'flex',
     flexDirection: 'column',
     gap: 16
    }}
   >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h3 style={{ margin: 0, color: '#fff', fontSize: 18, fontWeight: 900 }}>
      {initialItem ? 'Edit Exam Schedule' : 'Add Exam Schedule'}
     </h3>
          <button onClick={onDismiss} style={{ background: 'transparent', border: 'none', color: '#fff', cursor: 'pointer' }}>
      <X size={20} />
     </button>
    </div>

    {/* Calendar Date Picker — no manual typing */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <label style={{ fontSize: 11, fontWeight: 700, color: '#d0bcff' }}>Select Date (Calendar)</label>
     <input
      type="date"
      value={inputDate}
      onChange={(e) => setInputDate(e.target.value)}
            style={{
       width: '100%',
       padding: '12px 14px',
       borderRadius: 12,
       background: 'rgba(208, 188, 255, 0.1)',
       border: '1.5px solid rgba(208, 188, 255, 0.4)',
       color: '#fff',
       fontSize: 15,
       fontWeight: 700,
       colorScheme: 'dark',
       boxSizing: 'border-box'
      }}
     />
     {inputDate && (
            <div style={{ display: 'flex', gap: 10, marginTop: 4 }}>
              <span style={{ padding: '4px 12px', borderRadius: 8, background: 'rgba(208,188,255,0.15)', color: '#d0bcff', fontSize: 12, fontWeight: 800 }}>
        {displayDate}
       </span>
              <span style={{ padding: '4px 12px', borderRadius: 8, background: 'rgba(255,255,255,0.08)', color: '#ccc', fontSize: 12, fontWeight: 700 }}>
        {dayName}
       </span>
      </div>
     )}
    </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <label style={{ fontSize: 11, fontWeight: 700, color: '#d0bcff' }}>Exam Name(s)</label>
      <button
       onClick={() => setExamsState((prev) => [...prev, { baseName: '', mcq: '', written: '' }])}
              style={{ background: 'transparent', border: 'none', color: '#d0bcff', fontSize: 12, fontWeight: 800, cursor: 'pointer' }}
      >
       + Add Exam
      </button>
     </div>

     {examsState.map((ex, idx) => (
            <div key={idx} style={{ display: 'flex', flexDirection: 'column', gap: 6, background: 'rgba(255,255,255,0.03)', padding: 10, borderRadius: 12, border: '1px solid rgba(255,255,255,0.08)' }}>
              <div style={{ display: 'flex', gap: 6 }}>
        <input
         type="text"
         placeholder="e.g. C-01 Part-01"
         value={ex.baseName}
         onChange={(e) => {
          const val = e.target.value;
          setExamsState((prev) => prev.map((item, i) => (i === idx ? { ...item, baseName: val } : item)));
         }}
                  style={{ flex: 1, padding: 8, borderRadius: 8, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.2)', color: '#fff', fontSize: 13 }}
        />
        {examsState.length > 1 && (
         <button
          onClick={() => setExamsState((prev) => prev.filter((_, i) => i !== idx))}
                    style={{ background: 'rgba(239,83,80,0.15)', border: 'none', color: '#ef5350', borderRadius: 8, width: 32, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
         >
          <Trash2 size={14} />
         </button>
        )}
       </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        <input
         type="number"
         placeholder="MCQ Marks (e.g. 15)"
         value={ex.mcq}
         onChange={(e) => {
          const val = e.target.value;
          setExamsState((prev) => prev.map((item, i) => (i === idx ? { ...item, mcq: val } : item)));
         }}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: 8, background: 'rgba(0, 229, 255, 0.05)', border: '1px solid rgba(0, 229, 255, 0.3)', color: '#fff', fontSize: 12 }}
        />
        <input
         type="number"
         placeholder="Written Marks (e.g. 10)"
         value={ex.written}
         onChange={(e) => {
          const val = e.target.value;
          setExamsState((prev) => prev.map((item, i) => (i === idx ? { ...item, written: val } : item)));
         }}
                  style={{ width: '100%', padding: '8px 10px', borderRadius: 8, background: 'rgba(255, 183, 77, 0.05)', border: '1px solid rgba(255, 183, 77, 0.3)', color: '#fff', fontSize: 12 }}
        />
       </div>
      </div>
     ))}
    </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <label style={{ fontSize: 11, fontWeight: 700, color: '#d0bcff' }}>Syllabus Breakdown Note(s)</label>
      <button
       onClick={() => setSyllabus((prev) => [...prev, ''])}
              style={{ background: 'transparent', border: 'none', color: '#d0bcff', fontSize: 12, fontWeight: 800, cursor: 'pointer' }}
      >
       + Add Topic
      </button>
     </div>

     {syllabus.map((s, idx) => (
            <div key={idx} style={{ display: 'flex', gap: 6 }}>
       <input
        type="text"
        placeholder="Syllabus coverage notes..."
        value={s}
        onChange={(e) => {
         const val = e.target.value;
         setSyllabus((prev) => prev.map((item, i) => (i === idx ? val : item)));
        }}
                style={{ flex: 1, padding: 8, borderRadius: 8, background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.2)', color: '#fff', fontSize: 13 }}
       />
       {syllabus.length > 1 && (
        <button
         onClick={() => setSyllabus((prev) => prev.filter((_, i) => i !== idx))}
                  style={{ background: 'rgba(239,83,80,0.15)', border: 'none', color: '#ef5350', borderRadius: 8, width: 32, cursor: 'pointer' }}
        >
         <Trash2 size={14} />
        </button>
       )}
      </div>
     ))}
    </div>

        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 8 }}>
          <button onClick={onDismiss} style={{ padding: '10px 18px', borderRadius: 10, background: 'transparent', border: '1px solid rgba(255,255,255,0.2)', color: '#fff', cursor: 'pointer' }}>
      Cancel
     </button>
     <button
      disabled={!inputDate}
      onClick={() => {
       if (!inputDate) return;
       
       // Base name এবং Marks জোড়া লাগিয়ে সেভ করার লজিক
       const cleanExams = examsState.map((exObj) => {
        let finalStr = exObj.baseName.trim();
        if (exObj.mcq && exObj.written) {
         finalStr += ` MCQ (${exObj.mcq}) + Written (${exObj.written})`;
        } else if (exObj.mcq) {
         finalStr += ` MCQ (${exObj.mcq})`;
        } else if (exObj.written) {
         finalStr += ` Written (${exObj.written})`;
        }
        return finalStr;
       }).filter(Boolean);

       const cleanSyl = syllabus.map((t) => t.trim()).filter(Boolean);
       
       onSave({
        date: displayDate,
        day: dayName,
        exams: cleanExams,
        syllabus: cleanSyl
       });
      }}
            style={{ padding: '10px 20px', borderRadius: 10, background: inputDate ? '#d0bcff' : 'rgba(208,188,255,0.3)', color: '#381e72', border: 'none', fontWeight: 800, cursor: inputDate ? 'pointer' : 'default' }}
     >
      Save Exam Schedule
     </button>
    </div>
   </div>
  </div>
 );
};
