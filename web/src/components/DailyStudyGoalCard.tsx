import React, { useState, useEffect, useRef } from 'react';
import { Play, Pause, Maximize2, ChevronRight, BarChart2 } from 'lucide-react';
import { SquigglyProgressIndicator } from './SquigglyProgressIndicator';
import { StudyPuppetAvatar } from './StudyPuppetAvatar';
import { storage } from '../services/storage';
import { sound } from '../services/audio';

interface DailyStudyGoalCardProps {
 goalHours: number;
 todayMinutes: number;
 onNavigateToStats: () => void;
 onOpenFullScreenTimer: () => void;
}

export const DailyStudyGoalCard: React.FC<DailyStudyGoalCardProps> = ({
 goalHours,
 todayMinutes,
 onNavigateToStats,
 onOpenFullScreenTimer
}) => {
 const [isTimerRunning, setIsTimerRunning] = useState(false);
 const [elapsedSecs, setElapsedSecs] = useState(0);
 const intervalRef = useRef<number | null>(null);

 useEffect(() => {
  if (isTimerRunning) {
   intervalRef.current = window.setInterval(() => {
    setElapsedSecs((prev) => prev + 1);
   }, 1000);
  } else {
   if (intervalRef.current !== null) {
    clearInterval(intervalRef.current);
    intervalRef.current = null;
   }
  }
  return () => {
   if (intervalRef.current !== null) {
    clearInterval(intervalRef.current);
    intervalRef.current = null;
   }
  };
 }, [isTimerRunning]);

 const currentHours = todayMinutes / 60;
 const progress = goalHours > 0 ? Math.min(1, currentHours / goalHours) : 0;
 const percentInt = goalHours > 0 ? Math.round((currentHours / goalHours) * 100) : 0;
 const isGoalAchieved = currentHours >= goalHours && goalHours > 0;

 const handleToggleTimer = (e: React.MouseEvent) => {
  e.stopPropagation();
  sound.playTick();
  if (isTimerRunning) {
   // Pause and save logged seconds if >= 60
   if (elapsedSecs >= 60) {
    const mins = Math.floor(elapsedSecs / 60);
    storage.logStudyMinutes(mins, 'Study Session');
    setElapsedSecs(0);
   }
   setIsTimerRunning(false);
  } else {
   setIsTimerRunning(true);
  }
 };

 const formatTimer = (totalSecs: number) => {
  const hrs = Math.floor(totalSecs / 3600);
  const m = Math.floor((totalSecs % 3600) / 60);
  const s = totalSecs % 60;
  if (hrs > 0) {
   return `${String(hrs).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
 };

 return (
  <div
   className="haze-card"
      style={{
    padding: '18px 20px',
    border: isGoalAchieved
     ? '1px solid rgba(255, 213, 79, 0.4)'
     : '0.5px solid rgba(208, 188, 255, 0.28)'
   }}
  >
   {/* Header Row */}
   <div
        style={{
     display: 'flex',
     alignItems: 'center',
     justifyContent: 'space-between',
     cursor: 'pointer'
    }}
    onClick={onNavigateToStats}
   >
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
     <div
            style={{
       width: 40,
       height: 40,
       borderRadius: '50%',
       background: isGoalAchieved ? 'rgba(255, 213, 79, 0.18)' : 'rgba(208, 188, 255, 0.15)',
       display: 'flex',
       alignItems: 'center',
       justifyContent: 'center',
       border: isGoalAchieved ? '1px solid rgba(255, 213, 79, 0.4)' : '1px solid rgba(208, 188, 255, 0.3)'
      }}
     >
      <StudyPuppetAvatar
       studyHours={currentHours}
       strokeColor={isGoalAchieved ? '#ffd54f' : '#d0bcff'}
       isStudying={isTimerRunning}
       size={36}
      />
     </div>

     <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
       <span
                style={{
         fontSize: 11,
         fontWeight: 800,
         color: isGoalAchieved ? '#ffd54f' : '#d0bcff',
         letterSpacing: '1px'
        }}
       >
        DAILY STUDY GOAL
       </span>
       {isGoalAchieved && (
        <span
                  style={{
          fontSize: 10,
          fontWeight: 800,
          padding: '1px 6px',
          borderRadius: 6,
          background: 'rgba(255, 213, 79, 0.2)',
          color: '#ffd54f',
          border: '0.5px solid #ffd54f'
         }}
        >
         ACHIEVED
        </span>
       )}
      </div>

            <div style={{ fontSize: 13, fontWeight: 700, color: '#fff', marginTop: 2 }}>
       {currentHours.toFixed(1)}h / {goalHours}.0h ({percentInt}%)
      </div>
     </div>
    </div>

    {/* Stats Button */}
    <div
          style={{
      display: 'flex',
      alignItems: 'center',
      gap: 4,
      padding: '4px 10px',
      borderRadius: 'var(--radius-sm)',
      background: 'rgba(208, 188, 255, 0.12)',
      color: '#d0bcff',
      fontSize: 12,
      fontWeight: 700
     }}
    >
     <BarChart2 size={13} />
     <span>Stats</span>
     <ChevronRight size={14} />
    </div>
   </div>

   {/* Squiggly Progress Line */}
      <div style={{ margin: '14px 0 12px 0' }}>
    <SquigglyProgressIndicator
     progress={progress}
     color={isGoalAchieved ? '#ffd54f' : '#d0bcff'}
     trackColor="rgba(255, 255, 255, 0.15)"
    />
   </div>

   {/* Live Focus Timer Bar */}
   <div
        style={{
     display: 'flex',
     alignItems: 'center',
     justifyContent: 'space-between',
     padding: '10px 14px',
     borderRadius: 'var(--radius-md)',
     background: 'rgba(255, 255, 255, 0.04)',
     border: '0.5px solid rgba(255, 255, 255, 0.08)'
    }}
   >
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
     <button
      onClick={handleToggleTimer}
            style={{
       width: 34,
       height: 34,
       borderRadius: '50%',
       border: 'none',
       background: isTimerRunning ? '#f43f5e' : '#d0bcff',
       color: isTimerRunning ? '#fff' : '#1d1b20',
       display: 'flex',
       alignItems: 'center',
       justifyContent: 'center',
       cursor: 'pointer',
       boxShadow: isTimerRunning ? '0 0 12px rgba(244, 63, 94, 0.4)' : 'none'
      }}
     >
            {isTimerRunning ? <Pause size={16} /> : <Play size={16} style={{ marginLeft: 2 }} />}
     </button>

     <div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 16, fontWeight: 800, color: '#fff' }}>
       {formatTimer(elapsedSecs)}
      </div>
            <div style={{ fontSize: 10.5, color: isTimerRunning ? '#81c784' : 'rgba(255,255,255,0.5)', fontWeight: 600 }}>
       {isTimerRunning ? '● Live Focusing...' : 'Timer Ready'}
      </div>
     </div>
    </div>

    <button
     onClick={onOpenFullScreenTimer}
     title="Fullscreen Focus"
          style={{
      padding: '6px 12px',
      borderRadius: 'var(--radius-sm)',
      border: '0.5px solid rgba(255, 255, 255, 0.15)',
      background: 'rgba(255, 255, 255, 0.06)',
      color: '#d0bcff',
      fontSize: 12,
      fontWeight: 700,
      display: 'flex',
      alignItems: 'center',
      gap: 6,
      cursor: 'pointer'
     }}
    >
     <Maximize2 size={13} />
     <span>Focus</span>
    </button>
   </div>
  </div>
 );
};
