import React from 'react';
import {
  LayoutDashboard,
  ListTodo,
  FileSpreadsheet,
  TrendingUp
} from 'lucide-react';
import { sound } from '../services/audio';

export type ScreenRoute = 'home' | 'syllabus' | 'exams' | 'progress';

interface FloatingNavbarProps {
  currentRoute: ScreenRoute;
  onNavigate: (route: ScreenRoute) => void;
}

export const FloatingNavbar: React.FC<FloatingNavbarProps> = ({
  currentRoute,
  onNavigate
}) => {
  const items = [
    { route: 'home' as ScreenRoute, title: 'Home', icon: LayoutDashboard },
    { route: 'syllabus' as ScreenRoute, title: 'Syllabus', icon: ListTodo },
    { route: 'exams' as ScreenRoute, title: 'Exams', icon: FileSpreadsheet },
    { route: 'progress' as ScreenRoute, title: 'Progress', icon: TrendingUp }
  ];

  return (
    <div
      style={{
        position: 'fixed',
        bottom: 24,
        left: 0,
        right: 0,
        zIndex: 50,
        display: 'flex',
        justifyContent: 'center',
        padding: '0 16px',
        pointerEvents: 'none'
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: 480,
          height: 56,
          display: 'flex',
          alignItems: 'center',
          gap: 2, // Connected gap from video/FloatingNavbar.kt
          pointerEvents: 'auto'
        }}
      >
        {items.map((item) => {
          const isSelected = currentRoute === item.route;
          const Icon = item.icon;

          return (
            <a
              key={item.route}
              href={`#${item.route}`}
              onClick={(e) => {
                // If standard click without modifiers, handle smoothly
                if (!e.ctrlKey && !e.metaKey && !e.shiftKey) {
                  e.preventDefault();
                  sound.playTick();
                  window.location.hash = `#${item.route}`;
                  onNavigate(item.route);
                }
              }}
              style={{
                flex: isSelected ? 1.5 : 1,
                height: '100%',
                borderRadius: isSelected ? '28px' : '12px',
                background: isSelected ? '#cac4d0' : '#49454f', // Solid colors from FloatingNavbar.kt
                color: isSelected ? '#1d1b20' : '#cac4d0',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
                transition: 'all 0.3s cubic-bezier(0.16, 1, 0.3, 1)',
                padding: '0 8px',
                overflow: 'hidden',
                textDecoration: 'none',
                userSelect: 'none'
              }}
            >
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                  whiteSpace: 'nowrap'
                }}
              >
                <Icon size={20} color={isSelected ? '#1d1b20' : '#cac4d0'} />
                {isSelected && (
                  <span
                    style={{
                      fontSize: 12.5,
                      fontWeight: 700,
                      letterSpacing: '0.3px'
                    }}
                  >
                    {item.title}
                  </span>
                )}
              </div>
            </a>
          );
        })}
      </div>
    </div>
  );
};
