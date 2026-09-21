import React, { useState } from 'react';
import {
 CalendarDays,
 Search,
 CheckCircle2,
 Circle,
 BookOpen,
 GraduationCap,
 Sparkles
} from 'lucide-react';
import { RoutineItem } from '../types';
import { storage } from '../services/storage';
import { sound } from '../services/audio';

interface RoutineViewProps {
 routines: RoutineItem[];
}

export const RoutineView: React.FC<RoutineViewProps> = ({ routines }) => {
 const [searchQuery, setSearchQuery] = useState('');
 const [filterType, setFilterType] = useState<'all' | 'class_only' | 'exam_only' | 'completed'>('all');

 const handleToggle = (id: string, currentStatus?: boolean) => {
  const nextStatus = !currentStatus;
  storage.toggleRoutineItem(id, nextStatus);
  if (nextStatus) {
   sound.playSuccess();
  } else {
   sound.playTick();
  }
 };

 const filtered = routines.filter((r) => {
  const query = searchQuery.toLowerCase();
  const matchesSearch =
   r.date.toLowerCase().includes(query) ||
   r.day.toLowerCase().includes(query) ||
   (r.classSubject && r.classSubject.toLowerCase().includes(query)) ||
   (r.examDetails && r.examDetails.toLowerCase().includes(query)) ||
   r.topics.some((t) => t.toLowerCase().includes(query));

  if (!matchesSearch) return false;

  if (filterType === 'class_only') return !!r.classSubject;
  if (filterType === 'exam_only') return !!r.examDetails;
  if (filterType === 'completed') return !!r.isCompleted;

  return true;
 });

 const totalTasks = routines.length;
 const completedTasks = routines.filter((r) => r.isCompleted).length;
 const percent = totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0;

 return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
   {/* Header Banner */}
      <div className="glass-card" style={{ padding: '20px 24px' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: 14 }}>
     <div>
            <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--accent-violet)', letterSpacing: 1 }}>
       ADMISSION BATCH SCHEDULE
      </div>
            <h2 style={{ margin: '4px 0 0 0', fontSize: 22, fontWeight: 800, color: '#fff' }}>
       Class Schedule & Routine
      </h2>
     </div>

     <div
            style={{
       display: 'flex',
       alignItems: 'center',
       gap: 10,
       background: 'rgba(255, 255, 255, 0.05)',
       padding: '8px 16px',
       borderRadius: 'var(--radius-md)',
       border: '1px solid var(--border-glass)'
      }}
     >
      <div>
              <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>Progress</div>
              <div style={{ fontSize: 14, fontWeight: 700, color: '#fff' }}>
        {completedTasks}/{totalTasks} Days ({percent}%)
       </div>
      </div>
      <div
              style={{
        width: 32,
        height: 32,
        borderRadius: '50%',
        background: 'rgba(139, 92, 246, 0.2)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'var(--accent-violet)'
       }}
      >
       <Sparkles size={16} />
      </div>
     </div>
    </div>

    {/* Search & Filter Options */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginTop: 18 }}>
          <div style={{ position: 'relative', flex: 1, minWidth: 240 }}>
      <Search
       size={15}
              style={{
        position: 'absolute',
        left: 12,
        top: '50%',
        transform: 'translateY(-50%)',
        color: 'var(--text-muted)'
       }}
      />
      <input
       type="text"
       placeholder="Search by topic, chapter, date (e.g. ভেক্টর, P-01, Saturday)..."
       value={searchQuery}
       onChange={(e) => setSearchQuery(e.target.value)}
              style={{ width: '100%', paddingLeft: 34, fontSize: 13 }}
      />
     </div>

     <select
      value={filterType}
      onChange={(e) => setFilterType(e.target.value as any)}
            style={{ padding: '8px 14px', fontSize: 13 }}
     >
      <option value="all">All Days</option>
      <option value="class_only">Lectures Only</option>
      <option value="exam_only">Exams Only</option>
      <option value="completed">Completed Days</option>
     </select>
    </div>
   </div>

   {/* Routine Cards List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
    {filtered.length === 0 ? (
          <div className="glass-card" style={{ padding: 40, textAlign: 'center', color: 'var(--text-muted)' }}>
      No routine items found matching your search.
     </div>
    ) : (
     filtered.map((item) => {
      const hasClass = !!item.classSubject;
      const hasExam = !!item.examDetails;

      return (
       <div
        key={item.id}
        className="glass-card"
                style={{
         padding: '18px 22px',
         borderColor: item.isCompleted ? 'rgba(16, 185, 129, 0.4)' : 'var(--border-glass)',
         background: item.isCompleted ? 'rgba(16, 185, 129, 0.04)' : 'var(--bg-card)'
        }}
       >
                <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 14 }}>
         {/* Left Date / Day Pill */}
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 14, flex: 1 }}>
          <div
                      style={{
            padding: '8px 12px',
            borderRadius: 'var(--radius-md)',
            background: 'rgba(255, 255, 255, 0.05)',
            border: '1px solid var(--border-glass)',
            textAlign: 'center',
            minWidth: 70
           }}
          >
                      <div style={{ fontSize: 14, fontWeight: 800, color: '#fff' }}>{item.date}</div>
                      <div style={{ fontSize: 10.5, fontWeight: 600, color: 'var(--text-muted)' }}>
            {item.day}
           </div>
          </div>

                    <div style={{ flex: 1 }}>
           {/* Badges for Class and Exam */}
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 8 }}>
            {hasClass && (
             <span
                            style={{
               fontSize: 11,
               fontWeight: 700,
               padding: '2px 8px',
               borderRadius: 'var(--radius-sm)',
               background: 'rgba(0, 229, 255, 0.15)',
               color: 'var(--accent-cyan)',
               border: '1px solid rgba(0, 229, 255, 0.3)',
               display: 'inline-flex',
               alignItems: 'center',
               gap: 4
              }}
             >
              <GraduationCap size={12} />
              {item.classSubject}
             </span>
            )}

            {hasExam && (
             <span
                            style={{
               fontSize: 11,
               fontWeight: 700,
               padding: '2px 8px',
               borderRadius: 'var(--radius-sm)',
               background: 'rgba(245, 158, 11, 0.15)',
               color: 'var(--accent-amber)',
               border: '1px solid rgba(245, 158, 11, 0.3)',
               display: 'inline-flex',
               alignItems: 'center',
               gap: 4
              }}
             >
              <BookOpen size={12} />
              {item.examDetails}
             </span>
            )}
           </div>

           {/* Topics List */}
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            {item.topics.map((top, idx) => (
             <div
              key={idx}
                            style={{
               fontSize: 13.5,
               color: item.isCompleted ? 'var(--text-muted)' : 'var(--text-secondary)',
               lineHeight: 1.4,
               textDecoration: item.isCompleted ? 'line-through' : 'none'
              }}
             >
              • {top}
             </div>
            ))}
           </div>
          </div>
         </div>

         {/* Right Checkbox */}
         <button
          onClick={() => handleToggle(item.id, item.isCompleted)}
          title={item.isCompleted ? 'Mark pending' : 'Mark completed'}
                    style={{
           background: 'none',
           border: 'none',
           cursor: 'pointer',
           padding: 6,
           color: item.isCompleted ? 'var(--accent-emerald)' : 'var(--text-muted)',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center',
           transition: 'transform 0.18s ease'
          }}
         >
          {item.isCompleted ? <CheckCircle2 size={24} /> : <Circle size={24} />}
         </button>
        </div>
       </div>
      );
     })
    )}
   </div>
  </div>
 );
};
