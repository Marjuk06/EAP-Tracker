import React, { useState, useEffect } from 'react';
import { Target, Calendar, Clock } from 'lucide-react';

interface CountdownCardProps {
  targetExamName: string;
  targetExamEpoch: number;
  onEditCountdown: () => void;
}

export const CountdownCard: React.FC<CountdownCardProps> = ({
  targetExamName,
  targetExamEpoch,
  onEditCountdown
}) => {
  const [timeLeft, setTimeLeft] = useState<{
    days: number;
    hours: number;
    minutes: number;
    seconds: number;
    totalDaysInitial: number;
    progressPercent: number;
  }>({
    days: 0,
    hours: 0,
    minutes: 0,
    seconds: 0,
    totalDaysInitial: 150,
    progressPercent: 0
  });

  useEffect(() => {
    const updateCountdown = () => {
      const now = Date.now();
      const diff = Math.max(0, targetExamEpoch - now);

      const days = Math.floor(diff / (1000 * 60 * 60 * 24));
      const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diff % (1000 * 60)) / 1000);

      // Assume a default cycle of 180 days for progress ring
      const totalCycleDays = 180;
      const elapsedDays = Math.max(0, totalCycleDays - days);
      const progressPercent = Math.min(100, Math.max(0, Math.round((elapsedDays / totalCycleDays) * 100)));

      setTimeLeft({
        days,
        hours,
        minutes,
        seconds,
        totalDaysInitial: totalCycleDays,
        progressPercent
      });
    };

    updateCountdown();
    const interval = setInterval(updateCountdown, 1000);
    return () => clearInterval(interval);
  }, [targetExamEpoch]);

  const targetDateFormatted = new Date(targetExamEpoch).toLocaleDateString('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric'
  });

  return (
    <div className="glass-card" style={{ padding: '24px', position: 'relative' }}>
      {/* Background Radial Glow */}
      <div
        style={{
          position: 'absolute',
          top: '-20%',
          right: '-10%',
          width: '200px',
          height: '200px',
          background: 'radial-gradient(circle, rgba(0, 229, 255, 0.15) 0%, transparent 70%)',
          pointerEvents: 'none'
        }}
      />

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div
            style={{
              width: 36,
              height: 36,
              borderRadius: 10,
              background: 'rgba(0, 229, 255, 0.15)',
              border: '1px solid rgba(0, 229, 255, 0.3)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--accent-cyan)'
            }}
          >
            <Target size={20} />
          </div>
          <div>
            <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--accent-cyan)', letterSpacing: 1 }}>
              ADMISSION TARGET
            </div>
            <h3 style={{ margin: 0, fontSize: 17, fontWeight: 700, color: '#fff' }}>
              {targetExamName}
            </h3>
          </div>
        </div>

        <button
          onClick={onEditCountdown}
          className="btn-glass"
          style={{ padding: '6px 12px', fontSize: 12 }}
        >
          <Calendar size={14} />
          <span>Edit Target</span>
        </button>
      </div>

      {/* Grid of countdown boxes */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(4, 1fr)',
          gap: 10,
          marginTop: 18
        }}
      >
        {[
          { label: 'DAYS', value: timeLeft.days, color: 'var(--accent-cyan)' },
          { label: 'HOURS', value: timeLeft.hours, color: 'var(--accent-violet)' },
          { label: 'MINUTES', value: timeLeft.minutes, color: 'var(--accent-blue)' },
          { label: 'SECONDS', value: timeLeft.seconds, color: 'var(--accent-emerald)' }
        ].map((item) => (
          <div
            key={item.label}
            style={{
              background: 'rgba(255, 255, 255, 0.04)',
              border: '1px solid var(--border-glass)',
              borderRadius: 'var(--radius-md)',
              padding: '14px 8px',
              textAlign: 'center',
              position: 'relative',
              overflow: 'hidden'
            }}
          >
            <div
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: 28,
                fontWeight: 800,
                color: item.color,
                lineHeight: 1
              }}
            >
              {String(item.value).padStart(2, '0')}
            </div>
            <div
              style={{
                fontSize: 10.5,
                fontWeight: 700,
                color: 'var(--text-muted)',
                marginTop: 6,
                letterSpacing: 1
              }}
            >
              {item.label}
            </div>
          </div>
        ))}
      </div>

      {/* Target Exam Date Banner */}
      <div
        style={{
          marginTop: 16,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '10px 14px',
          borderRadius: 'var(--radius-sm)',
          background: 'rgba(255, 255, 255, 0.03)',
          border: '1px solid var(--border-glass)',
          fontSize: 12.5,
          color: 'var(--text-secondary)'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <Clock size={14} color="var(--accent-cyan)" />
          <span>Exam Expected Date: <strong style={{ color: '#fff' }}>{targetDateFormatted}</strong></span>
        </div>
        <span style={{ color: 'var(--accent-cyan)', fontWeight: 600 }}>
          {timeLeft.days} Days Left
        </span>
      </div>
    </div>
  );
};
