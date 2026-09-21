import React, { useState } from 'react';
import { ArrowLeft, Calendar, Flame, BarChart2 } from 'lucide-react';
import { DayStudyLog, StudySession } from '../types';
import { sound } from '../services/audio';

interface StudyStatisticsScreenProps {
 studyLogs: DayStudyLog[];
 sessions: StudySession[];
 streak: number;
 onBack: () => void;
}

export const StudyStatisticsScreen: React.FC<StudyStatisticsScreenProps> = ({
 studyLogs,
 sessions,
 streak,
 onBack
}) => {
 const [range, setRange] = useState<'7' | '30'>('7');

 const numDays = parseInt(range, 10);
 const logMap = new Map<string, number>();
 studyLogs.forEach((l) => logMap.set(l.date, l.minutes));

 const daysArray: { dateStr: string; label: string; minutes: number }[] = [];
 let totalMinutes = 0;

 for (let i = numDays - 1; i >= 0; i--) {
  const d = new Date();
  d.setDate(d.getDate() - i);
  const dateStr = d.toISOString().split('T')[0];
  const dayName = d.toLocaleDateString('en-US', { weekday: 'short' });
  const dayNum = d.getDate();
  const label = numDays === 7 ? dayName : `${dayNum}`;
  const minutes = logMap.get(dateStr) || 0;
  totalMinutes += minutes;
  daysArray.push({ dateStr, label, minutes });
 }

 const maxMinutes = Math.max(60, ...daysArray.map((d) => d.minutes));
 const totalHours = (totalMinutes / 60).toFixed(1);
 const avgHours = (totalMinutes / (numDays * 60)).toFixed(1);

 // Subject distribution
 const subjectMap = new Map<string, number>();
 sessions.forEach((s) => {
  const sub = s.subject || 'General Study';
  subjectMap.set(sub, (subjectMap.get(sub) || 0) + s.durationMinutes);
 });

 const subjectStats = Array.from(subjectMap.entries()).sort((a, b) => b[1] - a[1]);

 return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16, maxWidth: 640, margin: '0 auto' }}>
   {/* Top Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '4px 0' }}>
    <button
     onClick={() => {
      sound.playTick();
      onBack();
     }}
          style={{
      width: 40,
      height: 40,
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
     STUDY STATISTICS
    </h2>

        <div style={{ width: 40 }} />
   </div>

   {/* Overview Metric Row */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        <div className="haze-card" style={{ padding: '16px', borderRadius: '20px' }}>
          <div style={{ fontSize: 11, fontWeight: 700, color: '#d0bcff' }}>TOTAL STUDY TIME ({range}D)</div>
          <div style={{ fontFamily: 'var(--font-mono)', fontSize: 24, fontWeight: 800, color: '#fff', marginTop: 4 }}>
      {totalHours}h
     </div>
          <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.6)' }}>
      Daily Avg: {avgHours}h / day
     </div>
    </div>

        <div className="haze-card" style={{ padding: '16px', borderRadius: '20px' }}>
          <div style={{ fontSize: 11, fontWeight: 700, color: '#ffd54f' }}>ACTIVE STREAK</div>
          <div style={{ fontFamily: 'var(--font-mono)', fontSize: 24, fontWeight: 800, color: '#ffd54f', marginTop: 4 }}>
      {streak} Days
     </div>
          <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.6)' }}>
      Consistency Meter
     </div>
    </div>
   </div>

   {/* Timeline Chart Card */}
      <div className="haze-card" style={{ padding: '20px', borderRadius: '26px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <span style={{ fontSize: 14, fontWeight: 800, color: '#fff' }}>Daily Study Hours</span>

          <div style={{ display: 'flex', gap: 4 }}>
      {(['7', '30'] as const).map((r) => (
       <button
        key={r}
        onClick={() => {
         sound.playTick();
         setRange(r);
        }}
                style={{
         padding: '4px 10px',
         borderRadius: '8px',
         border: range === r ? '0.5px solid #d0bcff' : '0.5px solid rgba(255, 255, 255, 0.1)',
         background: range === r ? 'rgba(208, 188, 255, 0.2)' : 'transparent',
         color: range === r ? '#d0bcff' : 'rgba(255,255,255,0.6)',
         fontSize: 11.5,
         fontWeight: 700,
         cursor: 'pointer'
        }}
       >
        {r}D
       </button>
      ))}
     </div>
    </div>

    {/* Bar Chart */}
        <div style={{ display: 'flex', alignItems: 'flex-end', gap: range === '7' ? 12 : 3, height: 140, paddingTop: 16 }}>
     {daysArray.map((day, idx) => {
      const h = (day.minutes / 60).toFixed(1);
      const heightPercent = Math.max(6, Math.round((day.minutes / maxMinutes) * 100));

      return (
       <div
        key={idx}
                style={{
         flex: 1,
         display: 'flex',
         flexDirection: 'column',
         alignItems: 'center',
         gap: 6,
         height: '100%',
         justifyContent: 'flex-end'
        }}
       >
        {day.minutes > 0 && (
                  <span style={{ fontSize: range === '7' ? 9.5 : 7.5, fontWeight: 700, color: '#d0bcff' }}>
          {h}h
         </span>
        )}
        <div
                  style={{
          width: '100%',
          maxWidth: range === '7' ? 32 : 12,
          height: `${heightPercent}%`,
          borderRadius: '4px 4px 0 0',
          background: day.minutes > 0 ? '#d0bcff' : 'rgba(255, 255, 255, 0.08)'
         }}
        />
                <span style={{ fontSize: range === '7' ? 10 : 8, color: 'rgba(255,255,255,0.5)', fontWeight: 600 }}>
         {day.label}
        </span>
       </div>
      );
     })}
    </div>
   </div>

   {/* Subject Breakdown Card */}
   {subjectStats.length > 0 && (
        <div className="haze-card" style={{ padding: '20px', borderRadius: '26px' }}>
          <h3 style={{ margin: '0 0 14px 0', fontSize: 15, fontWeight: 800, color: '#fff' }}>
      Subject Time Distribution
     </h3>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {subjectStats.map(([subject, mins]) => {
       const hours = (mins / 60).toFixed(1);
       const totalM = subjectStats.reduce((acc, [, m]) => acc + m, 0);
       const p = totalM > 0 ? Math.round((mins / totalM) * 100) : 0;

       return (
        <div key={subject}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 4 }}>
                    <span style={{ fontWeight: 700, color: '#fff' }}>{subject}</span>
                    <span style={{ color: 'rgba(255,255,255,0.6)' }}>{hours}h ({p}%)</span>
         </div>
                  <div style={{ width: '100%', height: 4, background: 'rgba(255, 255, 255, 0.08)', borderRadius: 4, overflow: 'hidden' }}>
                    <div style={{ width: `${p}%`, height: '100%', background: '#d0bcff', borderRadius: 4 }} />
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
