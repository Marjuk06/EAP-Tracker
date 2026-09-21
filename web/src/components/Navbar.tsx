import React from 'react';
import {
  LayoutDashboard,
  BookOpen,
  CalendarDays,
  Award,
  Timer,
  BarChart3,
  User,
  Flame,
  Zap,
  Rocket,
  Target,
  Lightbulb,
  GraduationCap,
  Shield,
  Sparkles
} from 'lucide-react';
import { sound } from '../services/audio';

export type TabId = 'home' | 'syllabus' | 'routine' | 'exams' | 'timer' | 'stats' | 'profile';

interface NavbarProps {
  activeTab: TabId;
  onTabChange: (tab: TabId) => void;
  userName: string;
  avatarEmoji: string;
  streak: number;
}

const iconMap: Record<string, React.FC<{ size?: number; color?: string }>> = {
  Zap,
  Rocket,
  Target,
  Flame,
  BookOpen,
  Lightbulb,
  GraduationCap,
  Award,
  Shield,
  Sparkles
};

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  onTabChange,
  userName,
  avatarEmoji,
  streak
}) => {
  const tabs = [
    { id: 'home' as TabId, label: 'Dashboard', icon: LayoutDashboard },
    { id: 'syllabus' as TabId, label: 'Syllabus', icon: BookOpen },
    { id: 'routine' as TabId, label: 'Routine', icon: CalendarDays },
    { id: 'exams' as TabId, label: 'Exams', icon: Award },
    { id: 'timer' as TabId, label: 'Focus Timer', icon: Timer },
    { id: 'stats' as TabId, label: 'Statistics', icon: BarChart3 },
    { id: 'profile' as TabId, label: 'Profile', icon: User }
  ];

  const handleTabClick = (id: TabId) => {
    sound.playTick();
    onTabChange(id);
  };

  const AvatarIcon = iconMap[avatarEmoji] || User;

  return (
    <>
      {/* Desktop & Tablet Top Navbar */}
      <header
        style={{
          position: 'sticky',
          top: 0,
          zIndex: 50,
          background: 'rgba(9, 12, 20, 0.82)',
          backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
          borderBottom: '1px solid var(--border-glass)',
          padding: '12px 24px'
        }}
      >
        <div
          style={{
            maxWidth: 1280,
            margin: '0 auto',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 16
          }}
        >
          {/* Brand Logo & Title */}
          <div
            onClick={() => handleTabClick('home')}
            style={{ display: 'flex', alignItems: 'center', gap: 12, cursor: 'pointer' }}
          >
            <div
              style={{
                width: 40,
                height: 40,
                borderRadius: 'var(--radius-md)',
                background: 'linear-gradient(135deg, var(--accent-cyan), var(--accent-violet))',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 0 16px rgba(0, 229, 255, 0.35)',
                color: '#06101e'
              }}
            >
              <Zap size={22} color="#06101e" />
            </div>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span
                  style={{
                    fontFamily: 'var(--font-mono)',
                    fontWeight: 800,
                    fontSize: 18,
                    letterSpacing: 1.5,
                    background: 'linear-gradient(90deg, #fff, var(--accent-cyan))',
                    WebkitBackgroundClip: 'text',
                    WebkitTextFillColor: 'transparent'
                  }}
                >
                  EAP TRACKER
                </span>
                <span
                  style={{
                    fontSize: 10,
                    fontWeight: 700,
                    padding: '2px 6px',
                    borderRadius: 6,
                    background: 'rgba(0, 229, 255, 0.15)',
                    color: 'var(--accent-cyan)',
                    border: '1px solid rgba(0, 229, 255, 0.3)'
                  }}
                >
                  WEB
                </span>
              </div>
              <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                Admission & Engineering Tracker
              </div>
            </div>
          </div>

          {/* Desktop Nav Items */}
          <nav
            className="desktop-nav"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 6,
              background: 'rgba(255, 255, 255, 0.04)',
              padding: '4px 6px',
              borderRadius: 'var(--radius-full)',
              border: '1px solid var(--border-glass)'
            }}
          >
            {tabs.map((tab) => {
              const Icon = tab.icon;
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => handleTabClick(tab.id)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                    padding: '8px 16px',
                    borderRadius: 'var(--radius-full)',
                    border: 'none',
                    background: isActive
                      ? 'linear-gradient(135deg, rgba(0, 229, 255, 0.2), rgba(139, 92, 246, 0.25))'
                      : 'transparent',
                    color: isActive ? '#fff' : 'var(--text-secondary)',
                    fontWeight: isActive ? 600 : 500,
                    fontSize: 13.5,
                    cursor: 'pointer',
                    boxShadow: isActive ? '0 0 14px rgba(0, 229, 255, 0.25)' : 'none',
                    borderBottom: isActive ? '1px solid var(--accent-cyan)' : 'none',
                    transition: 'all 0.2s ease'
                  }}
                >
                  <Icon size={16} color={isActive ? 'var(--accent-cyan)' : 'currentColor'} />
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </nav>

          {/* Right Info Header */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
            {streak > 0 && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                  padding: '6px 12px',
                  borderRadius: 'var(--radius-full)',
                  background: 'rgba(245, 158, 11, 0.15)',
                  border: '1px solid rgba(245, 158, 11, 0.35)',
                  color: 'var(--accent-amber)',
                  fontSize: 13,
                  fontWeight: 700
                }}
              >
                <Flame size={16} color="var(--accent-amber)" />
                <span>{streak} Days Streak</span>
              </div>
            )}

            <div
              onClick={() => handleTabClick('profile')}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                padding: '4px 10px',
                borderRadius: 'var(--radius-full)',
                background: 'rgba(255, 255, 255, 0.05)',
                border: '1px solid var(--border-glass)',
                cursor: 'pointer'
              }}
            >
              <div
                style={{
                  width: 26,
                  height: 26,
                  borderRadius: '50%',
                  background: 'rgba(0, 229, 255, 0.2)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'var(--accent-cyan)'
                }}
              >
                <AvatarIcon size={15} />
              </div>
              <span style={{ fontSize: 13, fontWeight: 600, color: '#fff' }}>{userName}</span>
            </div>
          </div>
        </div>
      </header>

      {/* Mobile Bottom Navigation Bar */}
      <div
        className="mobile-bottom-nav"
        style={{
          position: 'fixed',
          bottom: 0,
          left: 0,
          right: 0,
          zIndex: 50,
          background: 'rgba(9, 12, 20, 0.92)',
          backdropFilter: 'blur(24px)',
          WebkitBackdropFilter: 'blur(24px)',
          borderTop: '1px solid var(--border-glass)',
          padding: '8px 12px',
          display: 'none',
          justifyContent: 'space-around',
          alignItems: 'center'
        }}
      >
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => handleTabClick(tab.id)}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 4,
                background: 'none',
                border: 'none',
                color: isActive ? 'var(--accent-cyan)' : 'var(--text-muted)',
                cursor: 'pointer',
                padding: '4px 8px',
                position: 'relative'
              }}
            >
              <div
                style={{
                  width: 36,
                  height: 36,
                  borderRadius: 12,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  background: isActive ? 'rgba(0, 229, 255, 0.15)' : 'transparent',
                  transition: 'all 0.2s ease'
                }}
              >
                <Icon size={20} />
              </div>
              <span style={{ fontSize: 10.5, fontWeight: isActive ? 700 : 500 }}>
                {tab.label}
              </span>
            </button>
          );
        })}
      </div>

      <style>{`
        @media (max-width: 768px) {
          .desktop-nav {
            display: none !important;
          }
          .mobile-bottom-nav {
            display: flex !important;
          }
        }
      `}</style>
    </>
  );
};
