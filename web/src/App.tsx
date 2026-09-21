import React, { useState, useEffect, useCallback } from 'react';
import { FloatingNavbar, ScreenRoute } from './components/FloatingNavbar';
import { HomeScreen } from './screens/HomeScreen';
import { SyllabusScreen } from './screens/SyllabusScreen';
import { ExamsScreen } from './screens/ExamsScreen';
import { ProgressScreen } from './screens/ProgressScreen';
import { ProfileScreen } from './screens/ProfileScreen';
import { FullScreenFocusTimerScreen } from './screens/FullScreenFocusTimerScreen';
import { StudyStatisticsScreen } from './screens/StudyStatisticsScreen';
import { FullRoutineScreen } from './screens/FullRoutineScreen';
import { FullExamScheduleScreen } from './screens/FullExamScheduleScreen';
import { AdminScreen, AdminTabId } from './screens/AdminScreen';

import { storage } from './services/storage';
import { sound } from './services/audio';
import type { Subject, UserProfile, RoutineItem, ExamRecord, DayStudyLog, StudySession } from './types';
import { Settings, Shield } from 'lucide-react';

export type AppNavDestination =
  | 'home'
  | 'syllabus'
  | 'exams'
  | 'progress'
  | 'profile'
  | 'fullscreen_focus'
  | 'study_statistics'
  | 'full_routine_offline'
  | 'full_routine_online'
  | 'full_exams_offline'
  | 'full_exams_online'
  | 'admin';

export const parseRouteFromHash = (hash: string): { screen: AppNavDestination; adminTab?: AdminTabId } => {
  const cleanHash = (hash || window.location.hash || '').replace(/^#\/?/, '').trim().toLowerCase();
  
  if (cleanHash.startsWith('admin')) {
    const parts = cleanHash.split('/');
    const sub = (parts[1] || 'routines') as AdminTabId;
    const validTabs: AdminTabId[] = ['routines', 'students', 'notifications', 'exams', 'quotes', 'syllabus', 'presets', 'cloud', 'help'];
    const activeTab = validTabs.includes(sub) ? sub : 'routines';
    return { screen: 'admin', adminTab: activeTab };
  }

  if (cleanHash === 'student' || cleanHash === 'preview' || cleanHash === 'home') return { screen: 'home' };
  if (cleanHash === 'syllabus') return { screen: 'syllabus' };
  if (cleanHash === 'exams') return { screen: 'exams' };
  if (cleanHash === 'progress') return { screen: 'progress' };
  if (cleanHash === 'profile' || cleanHash === 'settings') return { screen: 'profile' };
  if (cleanHash === 'focus' || cleanHash === 'fullscreen_focus') return { screen: 'fullscreen_focus' };
  if (cleanHash === 'stats' || cleanHash === 'study_statistics' || cleanHash === 'statistics') return { screen: 'study_statistics' };
  if (cleanHash === 'routine/offline' || cleanHash === 'full_routine_offline' || cleanHash === 'routine-offline') return { screen: 'full_routine_offline' };
  if (cleanHash === 'routine/online' || cleanHash === 'full_routine_online' || cleanHash === 'routine-online') return { screen: 'full_routine_online' };
  if (cleanHash === 'exams/offline' || cleanHash === 'full_exams_offline' || cleanHash === 'exams-offline') return { screen: 'full_exams_offline' };
  if (cleanHash === 'exams/online' || cleanHash === 'full_exams_online' || cleanHash === 'exams-online') return { screen: 'full_exams_online' };

  // By default, the Web Application is dedicated to the Master Admin Portal!
  return { screen: 'admin', adminTab: 'routines' };
};

export const getHashForScreen = (screen: AppNavDestination, adminTab?: AdminTabId): string => {
  switch (screen) {
    case 'home': return '#home';
    case 'syllabus': return '#syllabus';
    case 'exams': return '#exams';
    case 'progress': return '#progress';
    case 'profile': return '#profile';
    case 'fullscreen_focus': return '#fullscreen_focus';
    case 'study_statistics': return '#study_statistics';
    case 'full_routine_offline': return '#full_routine_offline';
    case 'full_routine_online': return '#full_routine_online';
    case 'full_exams_offline': return '#full_exams_offline';
    case 'full_exams_online': return '#full_exams_online';
    case 'admin': return adminTab ? `#admin/${adminTab}` : '#admin/routines';
  }
};

export const App: React.FC = () => {
  const initialRoute = parseRouteFromHash(window.location.hash);
  const [currentScreen, setCurrentScreen] = useState<AppNavDestination>(initialRoute.screen);
  const [adminTab, setAdminTab] = useState<AdminTabId>(initialRoute.adminTab || 'routines');

  const [profile, setProfile] = useState<UserProfile>(storage.getProfile());
  const [subjects, setSubjects] = useState<Subject[]>(storage.getSubjects());
  const [routines, setRoutines] = useState<RoutineItem[]>(storage.getRoutines());
  const [exams, setExams] = useState<ExamRecord[]>(storage.getExams());
  const [studyLogs, setStudyLogs] = useState<DayStudyLog[]>(storage.getStudyLogs());
  const [sessions, setSessions] = useState<StudySession[]>(storage.getStudySessions());
  const [todayMinutes, setTodayMinutes] = useState<number>(storage.getTodayStudyMinutes());
  const [streak, setStreak] = useState<number>(storage.getStudyStreak());

  // PWA Install Prompt state
  const [deferredPrompt, setDeferredPrompt] = useState<any>(null);
  const [canInstallApp, setCanInstallApp] = useState<boolean>(false);

  useEffect(() => {
    const handleBeforeInstall = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e);
      setCanInstallApp(true);
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstall);
    return () => window.removeEventListener('beforeinstallprompt', handleBeforeInstall);
  }, []);

  const handleInstallApp = async () => {
    if (deferredPrompt) {
      deferredPrompt.prompt();
      const choiceResult = await deferredPrompt.userChoice;
      if (choiceResult.outcome === 'accepted') {
        setCanInstallApp(false);
      }
      setDeferredPrompt(null);
    } else {
      alert('To install this app on your device:\n• Chrome / Edge: Click the "Install" icon in your URL bar.\n• iPhone Safari: Tap the Share button and select "Add to Home Screen".');
    }
  };

  // Global Navigation function that updates hash and triggers history
  const navigateTo = useCallback((screen: AppNavDestination, tab?: AdminTabId) => {
    const targetHash = getHashForScreen(screen, tab);
    if (window.location.hash !== targetHash) {
      window.location.hash = targetHash;
    }
    setCurrentScreen(screen);
    if (tab) setAdminTab(tab);
  }, []);

  // Listen to browser URL hash & popstate changes for back/forward support & direct links
  useEffect(() => {
    const syncRouteWithHash = () => {
      const parsed = parseRouteFromHash(window.location.hash);
      setCurrentScreen(parsed.screen);
      if (parsed.adminTab) {
        setAdminTab(parsed.adminTab);
      }
    };

    window.addEventListener('hashchange', syncRouteWithHash);
    window.addEventListener('popstate', syncRouteWithHash);
    return () => {
      window.removeEventListener('hashchange', syncRouteWithHash);
      window.removeEventListener('popstate', syncRouteWithHash);
    };
  }, []);

  // Shortcut Ctrl + Shift + A to toggle Admin Console
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.shiftKey && (e.key === 'A' || e.key === 'a')) {
        e.preventDefault();
        sound.playTick();
        if (currentScreen === 'admin') {
          navigateTo('home');
        } else {
          navigateTo('admin', adminTab || 'routines');
        }
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [currentScreen, adminTab, navigateTo]);

  useEffect(() => {
    const refreshData = () => {
      setProfile(storage.getProfile());
      setSubjects(storage.getSubjects());
      setRoutines(storage.getRoutines());
      setExams(storage.getExams());
      setStudyLogs(storage.getStudyLogs());
      setSessions(storage.getStudySessions());
      setTodayMinutes(storage.getTodayStudyMinutes());
      setStreak(storage.getStudyStreak());
    };

    window.addEventListener('storage', refreshData);
    const interval = setInterval(refreshData, 3000);
    return () => {
      window.removeEventListener('storage', refreshData);
      clearInterval(interval);
    };
  }, []);

  const isMainTab = ['home', 'syllabus', 'exams', 'progress'].includes(currentScreen);

  return (
    <div className="app-container" style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', position: 'relative' }}>
      {/* Background Animated Gradient Mesh */}
      <div className="m3-ambient-background" />

      {/* Top Floating Orb Accent */}
      <div
        style={{
          position: 'fixed',
          top: '-10%',
          left: '20%',
          width: '50vw',
          height: '50vw',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(208, 188, 255, 0.12) 0%, rgba(208, 188, 255, 0) 70%)',
          filter: 'blur(80px)',
          pointerEvents: 'none',
          zIndex: 0
        }}
      />

      {/* Secondary Bottom Orb */}
      <div
        style={{
          position: 'fixed',
          bottom: '-10%',
          right: '10%',
          width: '60vw',
          height: '60vw',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(204, 194, 220, 0.10) 0%, rgba(204, 194, 220, 0) 70%)',
          filter: 'blur(90px)',
          pointerEvents: 'none',
          zIndex: 0
        }}
      />

      {/* Center Subtle Violet Glow */}
      <div
        style={{
          position: 'fixed',
          top: '30%',
          left: '30%',
          width: '40vw',
          height: '40vw',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(102, 80, 164, 0.18) 0%, rgba(102, 80, 164, 0) 70%)',
          filter: 'blur(80px)',
          pointerEvents: 'none',
          zIndex: 0
        }}
      />

      {/* Top Header Floating Controls (Hidden in full Admin screen) */}
      {currentScreen !== 'admin' && (
        <header
          style={{
            position: 'relative',
            zIndex: 10,
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
            gap: 10,
            padding: '16px 20px 0',
            maxWidth: '1200px',
            margin: '0 auto',
            width: '100%'
          }}
        >
          {/* Discreet Admin Lock Button */}
          <button
            onClick={() => {
              sound.playTick();
              navigateTo('admin', adminTab || 'routines');
            }}
            title="Master Admin Console (Ctrl+Shift+A)"
            style={{
              width: 40,
              height: 40,
              borderRadius: '50%',
              border: '1px solid rgba(255, 255, 255, 0.12)',
              background: 'rgba(255, 255, 255, 0.05)',
              color: 'rgba(255, 255, 255, 0.45)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              backdropFilter: 'blur(12px)',
              transition: 'all 0.2s ease'
            }}
          >
            <Shield size={18} />
          </button>

          {/* Top-Right Settings Icon (Phone App Parity) */}
          {currentScreen !== 'profile' && (
            <button
              onClick={() => {
                sound.playTick();
                navigateTo('profile');
              }}
              style={{
                width: 44,
                height: 44,
                borderRadius: '50%',
                border: '1px solid rgba(255, 255, 255, 0.15)',
                background: 'rgba(255, 255, 255, 0.06)',
                color: '#fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
                backdropFilter: 'blur(12px)'
              }}
            >
              <Settings size={22} />
            </button>
          )}
        </header>
      )}

      {/* Main Content Area */}
      <main
        style={{
          position: 'relative',
          zIndex: 1,
          flex: 1,
          padding: currentScreen === 'admin' ? '16px 24px 60px' : '16px 16px 120px',
          maxWidth: currentScreen === 'admin' ? '100%' : '1200px',
          margin: currentScreen === 'admin' ? '0' : '0 auto',
          width: '100%',
          boxSizing: 'border-box'
        }}
      >
        {/* SCREEN 1: HOME */}
        {currentScreen === 'home' && (
          <HomeScreen
            goalHours={profile.dailyGoalHours}
            todayMinutes={todayMinutes}
            routines={routines}
            onNavigateToFullRoutine={(type) => navigateTo(type === 'online' ? 'full_routine_online' : 'full_routine_offline')}
            onNavigateToStudyStats={() => navigateTo('study_statistics')}
            onOpenFullScreenTimer={() => navigateTo('fullscreen_focus')}
            onNavigateToSettings={() => navigateTo('profile')}
          />
        )}

        {/* SCREEN 2: SYLLABUS */}
        {currentScreen === 'syllabus' && <SyllabusScreen subjects={subjects} />}

        {/* SCREEN 3: EXAMS */}
        {currentScreen === 'exams' && (
          <ExamsScreen
            exams={exams}
            onNavigateToFullExams={(type) => navigateTo(type === 'online' ? 'full_exams_online' : 'full_exams_offline')}
          />
        )}

        {/* SCREEN 4: PROGRESS */}
        {currentScreen === 'progress' && (
          <ProgressScreen
            subjects={subjects}
            exams={exams}
            studyLogs={studyLogs}
            onNavigateToStudyStats={() => navigateTo('study_statistics')}
          />
        )}

        {/* SUB-SCREEN: FULLSCREEN FOCUS TIMER */}
        {currentScreen === 'fullscreen_focus' && (
          <FullScreenFocusTimerScreen onBack={() => navigateTo('home')} />
        )}

        {/* SUB-SCREEN: STUDY STATISTICS */}
        {currentScreen === 'study_statistics' && (
          <StudyStatisticsScreen
            studyLogs={studyLogs}
            sessions={sessions}
            streak={streak}
            onBack={() => navigateTo('home')}
          />
        )}

        {/* SUB-SCREEN: PROFILE & SETTINGS */}
        {currentScreen === 'profile' && (
          <ProfileScreen
            profile={profile}
            onBack={() => navigateTo('home')}
            onNavigateToFullRoutine={(type) => navigateTo(type === 'online' ? 'full_routine_online' : 'full_routine_offline')}
            onNavigateToFullExams={(type) => navigateTo(type === 'online' ? 'full_exams_online' : 'full_exams_offline')}
          />
        )}

        {/* SUB-SCREEN: FULL ROUTINE */}
        {(currentScreen === 'full_routine_offline' || currentScreen === 'full_routine_online') && (
          <FullRoutineScreen
            type={currentScreen.includes('online') ? 'online' : 'offline'}
            routines={routines}
            onBack={() => navigateTo('home')}
          />
        )}

        {/* SUB-SCREEN: FULL EXAMS SCHEDULE */}
        {(currentScreen === 'full_exams_offline' || currentScreen === 'full_exams_online') && (
          <FullExamScheduleScreen
            type={currentScreen.includes('online') ? 'online' : 'offline'}
            exams={exams}
            onBack={() => navigateTo('exams')}
          />
        )}

        {/* MASTER ADMIN CONSOLE */}
        {currentScreen === 'admin' && (
          <AdminScreen
            onBack={() => navigateTo('home')}
            onOpenStudentPreview={() => navigateTo('home')}
            onInstallApp={handleInstallApp}
            canInstallApp={canInstallApp}
            initialTab={adminTab}
            onTabChange={(tab) => {
              setAdminTab(tab);
              const targetHash = `#admin/${tab}`;
              if (window.location.hash !== targetHash) window.location.hash = targetHash;
            }}
          />
        )}
      </main>

      {/* Floating Bottom Navbar */}
      {isMainTab && (
        <FloatingNavbar
          currentRoute={currentScreen as ScreenRoute}
          onNavigate={(route: ScreenRoute) => {
            sound.playTick();
            navigateTo(route as AppNavDestination);
          }}
        />
      )}
    </div>
  );
};

export default App;
