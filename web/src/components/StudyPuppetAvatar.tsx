import React from 'react';

interface StudyPuppetAvatarProps {
  studyHours: number;
  strokeColor?: string;
  isStudying?: boolean;
  size?: number;
}

export const StudyPuppetAvatar: React.FC<StudyPuppetAvatarProps> = ({
  studyHours,
  strokeColor = '#d0bcff',
  isStudying = false,
  size = 38
}) => {
  const isGodTier = studyHours >= 10;
  const isHardcore = studyHours >= 7;
  const isFocus = studyHours >= 3;

  return (
    <div
      style={{
        width: size,
        height: size,
        position: 'relative',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: '50%',
        overflow: 'hidden'
      }}
    >
      <svg width={size} height={size} viewBox="0 0 100 100">
        {/* Background glow if studying */}
        {isStudying && (
          <circle cx="50" cy="50" r="45" fill="rgba(208, 188, 255, 0.15)" />
        )}

        {/* Desk */}
        <line x1="15" y1="82" x2="85" y2="82" stroke={strokeColor} strokeWidth="4" strokeLinecap="round" />

        {/* Student Torso */}
        <path d="M 32 82 Q 50 68 68 82 Z" fill="rgba(208, 188, 255, 0.3)" stroke={strokeColor} strokeWidth="3" />

        {/* Student Head */}
        <circle cx="50" cy="46" r="18" fill="#1d1b20" stroke={strokeColor} strokeWidth="3.5" />

        {/* Bandana */}
        <path d="M 32 40 Q 50 36 68 40" stroke="#f43f5e" strokeWidth="4" strokeLinecap="round" />
        <circle cx="50" cy="40" r="2.5" fill="#fff" />
        {/* Bandana Knot Ribbon */}
        <path d="M 68 40 Q 76 44 78 52" stroke="#f43f5e" strokeWidth="3" strokeLinecap="round" fill="none" />

        {/* Eyes (Focused / Studying or Sleeping) */}
        {isStudying ? (
          <>
            <line x1="43" y1="48" x2="47" y2="48" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" />
            <line x1="53" y1="48" x2="57" y2="48" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" />
          </>
        ) : (
          <>
            <path d="M 43 49 Q 45 52 47 49" stroke="rgba(255,255,255,0.7)" strokeWidth="2" fill="none" />
            <path d="M 53 49 Q 55 52 57 49" stroke="rgba(255,255,255,0.7)" strokeWidth="2" fill="none" />
          </>
        )}

        {/* Study Lamp on Desk */}
        <path d="M 76 82 L 76 56 Q 76 48 68 50" stroke={strokeColor} strokeWidth="2.5" fill="none" />
        <path d="M 64 48 L 72 44 L 70 54 Z" fill="#ffd54f" />
        {/* Lamp light beam */}
        {isStudying && (
          <polygon points="68,54 44,82 66,82" fill="rgba(255, 213, 79, 0.25)" />
        )}

        {/* Auras based on level */}
        {isGodTier ? (
          <g>
            <path d="M 50 8 Q 44 17 46 22 Q 50 26 54 22 Q 56 17 50 8 Z" fill="#ff7043" />
            <circle cx="50" cy="19" r="2.5" fill="#ffd54f" />
          </g>
        ) : isHardcore ? (
          <g>
            <path d="M 44 16 Q 47 13 50 16 Q 53 19 56 16" stroke="#ffd54f" strokeWidth="2" fill="none" strokeLinecap="round" />
            <path d="M 46 20 Q 49 17 52 20 Q 55 23 58 20" stroke="#ffb74d" strokeWidth="1.5" fill="none" strokeLinecap="round" />
          </g>
        ) : isFocus ? (
          <polygon points="50,11 52,16 57,18 52,20 50,25 48,20 43,18 48,16" fill="#00e5ff" />
        ) : null}
      </svg>
    </div>
  );
};
