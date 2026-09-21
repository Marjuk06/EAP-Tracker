import React, { useState, useEffect, useRef } from 'react';
import { ArrowLeft, Play, Pause, RotateCcw, Check, Flame } from 'lucide-react';
import { StudyPuppetAvatar } from '../components/StudyPuppetAvatar';
import { storage } from '../services/storage';
import { sound } from '../services/audio';

interface FullScreenFocusTimerScreenProps {
  onBack: () => void;
}

export const FullScreenFocusTimerScreen: React.FC<FullScreenFocusTimerScreenProps> = ({
  onBack
}) => {
  const [isRunning, setIsRunning] = useState(false);
  const [seconds, setSeconds] = useState(0);
  const [selectedSubject, setSelectedSubject] = useState('Physics');
  const [loggedAlert, setLoggedAlert] = useState<string | null>(null);

  const intervalRef = useRef<number | null>(null);

  useEffect(() => {
    if (isRunning) {
      intervalRef.current = window.setInterval(() => {
        setSeconds((prev) => prev + 1);
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
  }, [isRunning]);

  const handleStartPause = () => {
    sound.playTick();
    setIsRunning((prev) => !prev);
  };

  const handleReset = () => {
    sound.playTick();
    setIsRunning(false);
    setSeconds(0);
  };

  const handleSaveSession = () => {
    sound.playSuccess();
    const durationMins = Math.max(1, Math.round(seconds / 60));
    storage.logStudyMinutes(durationMins, selectedSubject);
    setLoggedAlert(`Saved +${durationMins}m to Daily Goal!`);
    setTimeout(() => setLoggedAlert(null), 3500);
    handleReset();
  };

  const hours = Math.floor(seconds / 3600);
  const mins = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;
  const formattedTime = `${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

  const currentStudyHours = storage.getTodayStudyMinutes() / 60;

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'space-between',
        minHeight: '80vh',
        maxWidth: 500,
        margin: '0 auto',
        padding: '10px 16px'
      }}
    >
      {/* Top Header */}
      <div
        style={{
          width: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: 10
        }}
      >
        <button
          onClick={onBack}
          style={{
            width: 42,
            height: 42,
            borderRadius: '50%',
            border: '0.5px solid rgba(255, 255, 255, 0.15)',
            background: 'rgba(255, 255, 255, 0.06)',
            color: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer'
          }}
        >
          <ArrowLeft size={20} />
        </button>

        <h2 style={{ margin: 0, fontSize: 18, fontWeight: 900, color: '#fff', letterSpacing: '1px' }}>
          FOCUS TIMER
        </h2>

        <div style={{ width: 42 }} />
      </div>

      {/* Center Puppet & Giant Clock Visualizer */}
      <div
        className="haze-card"
        style={{
          width: '100%',
          padding: '36px 20px',
          borderRadius: '32px',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 20
        }}
      >
        <div
          style={{
            width: 100,
            height: 100,
            borderRadius: '50%',
            background: 'rgba(208, 188, 255, 0.15)',
            border: '1px solid rgba(208, 188, 255, 0.3)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
        >
          <StudyPuppetAvatar
            studyHours={currentStudyHours}
            isStudying={isRunning}
            size={90}
          />
        </div>

        {/* Digital Time Display */}
        <div
          style={{
            fontFamily: 'var(--font-mono)',
            fontSize: 48,
            fontWeight: 900,
            color: '#fff',
            letterSpacing: '2px'
          }}
        >
          {formattedTime}
        </div>

        <div
          style={{
            fontSize: 12,
            fontWeight: 800,
            color: isRunning ? '#81c784' : 'rgba(255,255,255,0.5)',
            letterSpacing: '1px'
          }}
        >
          {isRunning ? '● LIVE STUDY IN PROGRESS' : 'FOCUS PAUSED'}
        </div>

        {/* Subject Picker */}
        <select
          value={selectedSubject}
          onChange={(e) => setSelectedSubject(e.target.value)}
          style={{
            padding: '8px 16px',
            borderRadius: '12px',
            background: 'rgba(255, 255, 255, 0.06)',
            border: '0.5px solid rgba(255, 255, 255, 0.15)',
            color: '#fff',
            fontSize: 13,
            fontWeight: 600,
            outline: 'none',
            cursor: 'pointer'
          }}
        >
          <option value="Physics">Physics (পদার্থবিজ্ঞান)</option>
          <option value="Chemistry">Chemistry (রসায়ন)</option>
          <option value="Higher Math">Higher Math (উচ্চতর গণিত)</option>
          <option value="Biology">Biology (জীববিজ্ঞান)</option>
          <option value="General Study">General Revision</option>
        </select>
      </div>

      {/* Control Buttons */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, margin: '24px 0' }}>
        <button
          onClick={handleReset}
          style={{
            width: 50,
            height: 50,
            borderRadius: '50%',
            border: '0.5px solid rgba(255, 255, 255, 0.15)',
            background: 'rgba(255, 255, 255, 0.08)',
            color: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer'
          }}
        >
          <RotateCcw size={20} />
        </button>

        <button
          onClick={handleStartPause}
          style={{
            width: 76,
            height: 76,
            borderRadius: '50%',
            border: 'none',
            background: isRunning ? '#f43f5e' : '#d0bcff',
            color: isRunning ? '#fff' : '#1d1b20',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            boxShadow: isRunning ? '0 0 20px rgba(244, 63, 94, 0.5)' : '0 0 20px rgba(208, 188, 255, 0.4)'
          }}
        >
          {isRunning ? <Pause size={30} /> : <Play size={30} style={{ marginLeft: 3 }} />}
        </button>

        <button
          onClick={handleSaveSession}
          disabled={seconds < 10}
          style={{
            width: 50,
            height: 50,
            borderRadius: '50%',
            border: '0.5px solid rgba(255, 255, 255, 0.15)',
            background: seconds >= 10 ? 'rgba(129, 199, 132, 0.2)' : 'rgba(255, 255, 255, 0.04)',
            color: seconds >= 10 ? '#81c784' : 'rgba(255,255,255,0.3)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: seconds >= 10 ? 'pointer' : 'default'
          }}
        >
          <Check size={22} />
        </button>
      </div>

      {/* Logged Message Toast */}
      {loggedAlert && (
        <div
          style={{
            padding: '10px 18px',
            borderRadius: '16px',
            background: 'rgba(129, 199, 132, 0.25)',
            border: '1px solid #81c784',
            color: '#fff',
            fontSize: 13,
            fontWeight: 700
          }}
        >
          {loggedAlert}
        </div>
      )}
    </div>
  );
};
