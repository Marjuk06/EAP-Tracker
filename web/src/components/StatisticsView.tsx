import React, { useState } from 'react';
import {
 BarChart3,
 Flame,
 Clock,
 BookOpen,
 Calendar,
 Award,
 Sparkles
} from 'lucide-react';
import { DayStudyLog, StudySession } from '../types';

interface StatisticsViewProps {
 studyLogs: DayStudyLog[];
 sessions: StudySession[];
 streak: number;
}

export const StatisticsView: React.FC<StatisticsViewProps> = ({
 studyLogs,
 sessions,
 streak
}) => {
 const [range, setRange] = useState<'7' | '30'>('7');

 // Compute last N days
 const numDays = parseInt(range, 10);
 const daysArray: { dateStr: string; label: string; minutes: number }[] = [];

 const logMap = new Map<string, number>();
 studyLogs.forEach((l) => logMap.set(l.date, l.minutes));

 for (let i = numDays - 1; i >= 0; i--) {
  const d = new Date();
  d.setDate(d.getDate() - i);
  const dateStr = d.toISOString().split('T')[0];
  const dayName = d.toLocaleDateString('en-US', { weekday: 'short' });
  const dayNum = d.getDate();
  const label = numDays === 7 ? dayName : `${dayNum}`;
  const minutes = logMap.get(dateStr) || 0;
  daysArray.push({ dateStr, label, minutes });
 }

 const maxMinutes = Math.max(60, ...daysArray.map((d) => d.minutes));
 const totalMinutes = daysArray.reduce((acc, d) => acc + d.minutes, 0);
 const totalHours = (totalMinutes / 60).toFixed(1);
 const avgHours = (totalMinutes / (numDays * 60)).toFixed(1);

 // Subject distribution
 const subjectMap = new Map<string, number>();
 sessions.forEach((s) => {
  const sub = s.subject || 'General';
  subjectMap.set(sub, (subjectMap.get(sub) || 0) + s.durationMinutes);
 });

 const subjectStats = Array.from(subjectMap.entries()).sort((a, b) => b[1] - a[1]);

 return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
   {/* Top Banner */}
      <div className="glass-card" style={{ padding: '20px 24px' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: 14 }}>
     <div>
            <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--accent-cyan)', letterSpacing: 1 }}>
       STUDY TIME & HEATMAP
      </div>
            <h2 style={{ margin: '4px 0 0 0', fontSize: 22, fontWeight: 800, color: '#fff', display: 'flex', alignItems: 'center', gap: 8 }}>
       <BarChart3 size={22} color="var(--accent-cyan)" />
       <span>Study Statistics & Consistency</span>
      </h2>
     </div>

          <div style={{ display: 'flex', gap: 6 }}>
      <button
       onClick={() => setRange('7')}
              style={{
        padding: '6px 14px',
        borderRadius: 'var(--radius-sm)',
        border: range === '7' ? '1px solid var(--accent-cyan)' : '1px solid var(--border-glass)',
        background: range === '7' ? 'rgba(0, 229, 255, 0.2)' : 'transparent',
        color: range === '7' ? '#fff' : 'var(--text-secondary)',
        fontSize: 12.5,
        fontWeight: 600,
        cursor: 'pointer'
       }}
      >
       Past 7 Days
      </button>
      <button
       onClick={() => setRange('30')}
              style={{
        padding: '6px 14px',
        borderRadius: 'var(--radius-sm)',
        border: range === '30' ? '1px solid var(--accent-cyan)' : '1px solid var(--border-glass)',
        background: range === '30' ? 'rgba(0, 229, 255, 0.2)' : 'transparent',
        color: range === '30' ? '#fff' : 'var(--text-secondary)',
        fontSize: 12.5,
        fontWeight: 600,
        cursor: 'pointer'
       }}
      >
       Past 30 Days
      </button>
     </div>
    </div>

    {/* Quick Stat Cards */}
    <div
          style={{
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
      gap: 12,
      marginTop: 18
     }}
    >
     <div
            style={{
       padding: '14px 16px',
       borderRadius: 'var(--radius-md)',
       background: 'rgba(255, 255, 255, 0.04)',
       border: '1px solid var(--border-glass)'
      }}
     >
            <div style={{ fontSize: 11, color: 'var(--text-muted)', fontWeight: 600 }}>TOTAL TIME ({range}D)</div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 24, fontWeight: 800, color: '#fff', marginTop: 4 }}>
       {totalHours} Hours
      </div>
     </div>

     <div
            style={{
       padding: '14px 16px',
       borderRadius: 'var(--radius-md)',
       background: 'rgba(0, 229, 255, 0.08)',
       border: '1px solid rgba(0, 229, 255, 0.25)'
      }}
     >
            <div style={{ fontSize: 11, color: 'var(--accent-cyan)', fontWeight: 600 }}>DAILY AVERAGE</div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 24, fontWeight: 800, color: 'var(--accent-cyan)', marginTop: 4 }}>
       {avgHours} Hours/Day
      </div>
     </div>

     <div
            style={{
       padding: '14px 16px',
       borderRadius: 'var(--radius-md)',
       background: 'rgba(245, 158, 11, 0.08)',
       border: '1px solid rgba(245, 158, 11, 0.25)'
      }}
     >
            <div style={{ fontSize: 11, color: 'var(--accent-amber)', fontWeight: 600 }}>ACTIVE STREAK</div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 24, fontWeight: 800, color: 'var(--accent-amber)', marginTop: 4, display: 'flex', alignItems: 'center', gap: 6 }}>
       <Flame size={22} color="var(--accent-amber)" />
       <span>{streak} Days</span>
      </div>
     </div>
    </div>
   </div>

   {/* Bar Chart Card */}
      <div className="glass-card" style={{ padding: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
          <h3 style={{ margin: 0, fontSize: 17, fontWeight: 700, color: '#fff', display: 'flex', alignItems: 'center', gap: 8 }}>
      <BarChart3 size={18} color="var(--accent-cyan)" />
      <span>Study Hours Timeline</span>
     </h3>
    </div>

    {/* SVG Bar Chart */}
    <div
          style={{
      display: 'flex',
      alignItems: 'flex-end',
      gap: range === '7' ? 14 : 4,
      height: 180,
      paddingTop: 24,
      borderBottom: '1px solid var(--border-glass)'
     }}
    >
     {daysArray.map((day, idx) => {
      const h = (day.minutes / 60).toFixed(1);
      const heightPercent = Math.max(4, Math.round((day.minutes / maxMinutes) * 100));

      return (
       <div
        key={idx}
                style={{
         flex: 1,
         display: 'flex',
         flexDirection: 'column',
         alignItems: 'center',
         gap: 8,
         height: '100%',
         justifyContent: 'flex-end'
        }}
       >
        {day.minutes > 0 && (
                  <span style={{ fontSize: range === '7' ? 10 : 8, fontWeight: 700, color: 'var(--text-muted)' }}>
          {h}h
         </span>
        )}
        <div
         title={`${day.dateStr}: ${h} hours`}
                  style={{
          width: '100%',
          maxWidth: range === '7' ? 36 : 14,
          height: `${heightPercent}%`,
          borderRadius: '6px 6px 0 0',
          background: day.minutes > 0
           ? 'linear-gradient(180deg, var(--accent-cyan), #0077b6)'
           : 'rgba(255, 255, 255, 0.05)',
          boxShadow: day.minutes > 0 ? '0 0 12px rgba(0, 229, 255, 0.3)' : 'none',
          transition: 'all 0.3s ease'
         }}
        />
        <span
                  style={{
          fontSize: range === '7' ? 11 : 9,
          color: 'var(--text-muted)',
          fontWeight: 600,
          marginBottom: 4
         }}
        >
         {day.label}
        </span>
       </div>
      );
     })}
    </div>
   </div>

   {/* Subject Distribution Breakdown */}
   {subjectStats.length > 0 && (
        <div className="glass-card" style={{ padding: '20px 24px' }}>
          <h3 style={{ margin: '0 0 16px 0', fontSize: 17, fontWeight: 700, color: '#fff', display: 'flex', alignItems: 'center', gap: 8 }}>
      <BookOpen size={18} color="var(--accent-violet)" />
      <span>Subject Time Allocation</span>
     </h3>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {subjectStats.map(([subject, mins]) => {
       const hours = (mins / 60).toFixed(1);
       const totalM = subjectStats.reduce((acc, [, m]) => acc + m, 0);
       const p = totalM > 0 ? Math.round((mins / totalM) * 100) : 0;

       return (
        <div key={subject}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13.5, marginBottom: 6 }}>
                    <span style={{ fontWeight: 600, color: '#fff' }}>{subject}</span>
                    <span style={{ color: 'var(--text-muted)' }}>
           {hours}h ({p}%)
          </span>
         </div>
         <div
                    style={{
           width: '100%',
           height: 6,
           background: 'rgba(255, 255, 255, 0.06)',
           borderRadius: 'var(--radius-full)',
           overflow: 'hidden'
          }}
         >
          <div
                      style={{
            width: `${p}%`,
            height: '100%',
            background: 'linear-gradient(90deg, var(--accent-violet), var(--accent-cyan))',
            borderRadius: 'var(--radius-full)'
           }}
          />
         </div>
        </div>
       );
      })}
     </div>
    </div>
   )}
  </div>
 );
};
