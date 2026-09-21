import React, { useState, useEffect } from 'react';
import { X, Check } from 'lucide-react';
import { sound } from '../services/audio';

interface TimePickerModalProps {
  initialTime: string; // "08:00" in 24h or "08:00 AM"
  title: string;
  onSave: (timeString: string) => void;
  onClose: () => void;
}

export const TimePickerModal: React.FC<TimePickerModalProps> = ({
  initialTime,
  title,
  onSave,
  onClose
}) => {
  // Parse initial 24h time to 12h + AM/PM
  const parseTime = (t: string) => {
    const parts = t.split(':');
    let h = parseInt(parts[0] || '8', 10);
    const m = parseInt(parts[1] || '0', 10);
    const ampm = h >= 12 ? 'PM' : 'AM';
    h = h % 12;
    if (h === 0) h = 12;
    return { hour: h, minute: m, ampm };
  };

  const initialParsed = parseTime(initialTime);
  const [selectedHour, setSelectedHour] = useState<number>(initialParsed.hour);
  const [selectedMinute, setSelectedMinute] = useState<number>(initialParsed.minute);
  const [selectedAmPm, setSelectedAmPm] = useState<'AM' | 'PM'>(initialParsed.ampm as 'AM' | 'PM');

  // Compute countdown relative string: "Alert in X hours Y minutes"
  const [relativeText, setRelativeText] = useState<string>('');

  useEffect(() => {
    const now = new Date();
    let targetHour = selectedHour % 12;
    if (selectedAmPm === 'PM') targetHour += 12;

    const target = new Date();
    target.setHours(targetHour, selectedMinute, 0, 0);

    if (target.getTime() <= now.getTime()) {
      target.setDate(target.getDate() + 1);
    }

    const diffMs = target.getTime() - now.getTime();
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));

    if (diffHours === 0 && diffMinutes === 0) {
      setRelativeText('Alert in less than 1 minute');
    } else if (diffHours === 0) {
      setRelativeText(`Alert in ${diffMinutes} minutes`);
    } else if (diffMinutes === 0) {
      setRelativeText(`Alert in ${diffHours} hours`);
    } else {
      setRelativeText(`Alert in ${diffHours} hours ${diffMinutes} minutes`);
    }
  }, [selectedHour, selectedMinute, selectedAmPm]);

  const handleHourChange = (newHour: number) => {
    sound.playTick();
    // Check if crossing boundary (11 -> 12 or 12 -> 11) for Xiaomi auto AM/PM toggle
    if ((selectedHour === 11 && newHour === 12) || (selectedHour === 12 && newHour === 11)) {
      setSelectedAmPm((prev) => (prev === 'AM' ? 'PM' : 'AM'));
    }
    setSelectedHour(newHour);
  };

  const handleMinuteChange = (newMinute: number) => {
    sound.playTick();
    setSelectedMinute(newMinute);
  };

  const handleAmPmToggle = (val: 'AM' | 'PM') => {
    sound.playTick();
    setSelectedAmPm(val);
  };

  const handleSave = () => {
    sound.playSuccess();
    let h = selectedHour % 12;
    if (selectedAmPm === 'PM') h += 12;
    const time24 = `${String(h).padStart(2, '0')}:${String(selectedMinute).padStart(2, '0')}`;
    onSave(time24);
  };

  const hoursList = Array.from({ length: 12 }, (_, i) => i + 1);
  const minutesList = Array.from({ length: 60 }, (_, i) => i);

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 110,
        background: 'rgba(0, 0, 0, 0.75)',
        backdropFilter: 'blur(16px)',
        WebkitBackdropFilter: 'blur(16px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 20
      }}
      onClick={onClose}
    >
      <div
        className="glass-modal"
        style={{
          width: '100%',
          maxWidth: 380,
          padding: '24px 20px',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header with Cancel (X), Title + Relative countdown, and Save (Check) */}
        <div
          style={{
            width: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            marginBottom: 20
          }}
        >
          <button
            onClick={onClose}
            className="btn-glass"
            style={{ width: 36, height: 36, borderRadius: '50%', padding: 0 }}
          >
            <X size={18} />
          </button>

          <div style={{ textAlign: 'center' }}>
            <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: '#fff' }}>
              {title}
            </h3>
            <div style={{ fontSize: 11.5, color: 'var(--accent-cyan)', fontWeight: 600, marginTop: 2 }}>
              {relativeText}
            </div>
          </div>

          <button
            onClick={handleSave}
            className="btn-primary"
            style={{ width: 36, height: 36, borderRadius: '50%', padding: 0 }}
          >
            <Check size={18} />
          </button>
        </div>

        {/* Xiaomi Clock Wheel Container */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 16,
            width: '100%',
            height: 180,
            position: 'relative',
            background: 'rgba(255, 255, 255, 0.03)',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border-glass)',
            overflow: 'hidden'
          }}
        >
          {/* Highlight Selection Bar */}
          <div
            style={{
              position: 'absolute',
              top: '50%',
              left: 12,
              right: 12,
              height: 48,
              transform: 'translateY(-50%)',
              background: 'rgba(0, 229, 255, 0.12)',
              border: '1px solid rgba(0, 229, 255, 0.3)',
              borderRadius: 'var(--radius-md)',
              pointerEvents: 'none'
            }}
          />

          {/* Hour Column */}
          <div
            style={{
              height: '100%',
              overflowY: 'auto',
              width: 70,
              textAlign: 'center',
              scrollSnapType: 'y mandatory',
              padding: '66px 0'
            }}
          >
            {hoursList.map((h) => (
              <div
                key={h}
                onClick={() => handleHourChange(h)}
                style={{
                  height: 48,
                  lineHeight: '48px',
                  fontFamily: 'var(--font-mono)',
                  fontSize: selectedHour === h ? 24 : 16,
                  fontWeight: selectedHour === h ? 800 : 500,
                  color: selectedHour === h ? 'var(--accent-cyan)' : 'var(--text-muted)',
                  cursor: 'pointer',
                  scrollSnapAlign: 'center',
                  transition: 'all 0.15s ease'
                }}
              >
                {String(h).padStart(2, '0')}
              </div>
            ))}
          </div>

          <div style={{ fontFamily: 'var(--font-mono)', fontSize: 24, fontWeight: 800, color: 'var(--text-muted)' }}>
            :
          </div>

          {/* Minute Column */}
          <div
            style={{
              height: '100%',
              overflowY: 'auto',
              width: 70,
              textAlign: 'center',
              scrollSnapType: 'y mandatory',
              padding: '66px 0'
            }}
          >
            {minutesList.map((m) => (
              <div
                key={m}
                onClick={() => handleMinuteChange(m)}
                style={{
                  height: 48,
                  lineHeight: '48px',
                  fontFamily: 'var(--font-mono)',
                  fontSize: selectedMinute === m ? 24 : 16,
                  fontWeight: selectedMinute === m ? 800 : 500,
                  color: selectedMinute === m ? 'var(--accent-cyan)' : 'var(--text-muted)',
                  cursor: 'pointer',
                  scrollSnapAlign: 'center',
                  transition: 'all 0.15s ease'
                }}
              >
                {String(m).padStart(2, '0')}
              </div>
            ))}
          </div>

          {/* AM / PM Column */}
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              gap: 8,
              justifyContent: 'center'
            }}
          >
            {(['AM', 'PM'] as const).map((period) => (
              <button
                key={period}
                onClick={() => handleAmPmToggle(period)}
                style={{
                  padding: '8px 16px',
                  borderRadius: 'var(--radius-sm)',
                  border: selectedAmPm === period ? '1px solid var(--accent-violet)' : '1px solid var(--border-glass)',
                  background: selectedAmPm === period ? 'rgba(139, 92, 246, 0.25)' : 'transparent',
                  color: selectedAmPm === period ? '#fff' : 'var(--text-muted)',
                  fontWeight: 700,
                  fontSize: 13,
                  cursor: 'pointer'
                }}
              >
                {period}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
