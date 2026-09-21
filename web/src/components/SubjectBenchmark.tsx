import React from 'react';
import { Zap, FlaskConical, Calculator, Dna, ArrowRight } from 'lucide-react';
import { Subject } from '../types';
import { sound } from '../services/audio';

interface SubjectBenchmarkProps {
 subjects: Subject[];
 onOpenSubject: (subjectName: string) => void;
}

export const SubjectBenchmark: React.FC<SubjectBenchmarkProps> = ({
 subjects,
 onOpenSubject
}) => {
 const getIcon = (name: string) => {
  switch (name.toLowerCase()) {
   case 'physics':
    return <Zap size={18} color="var(--accent-cyan)" />;
   case 'chemistry':
    return <FlaskConical size={18} color="var(--accent-emerald)" />;
   case 'higher math':
    return <Calculator size={18} color="var(--accent-violet)" />;
   case 'biology':
    return <Dna size={18} color="var(--accent-rose)" />;
   default:
    return <Zap size={18} />;
  }
 };

 const getGradient = (name: string) => {
  switch (name.toLowerCase()) {
   case 'physics':
    return 'linear-gradient(90deg, #00e5ff, #00b4d8)';
   case 'chemistry':
    return 'linear-gradient(90deg, #10b981, #059669)';
   case 'higher math':
    return 'linear-gradient(90deg, #8b5cf6, #6d28d9)';
   case 'biology':
    return 'linear-gradient(90deg, #f43f5e, #be123c)';
   default:
    return 'linear-gradient(90deg, #00e5ff, #8b5cf6)';
  }
 };

 return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <h3 style={{ margin: 0, fontSize: 17, fontWeight: 700, color: '#fff' }}>
     Syllabus Benchmark Progress
    </h3>
        <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
     Realtime Completion
    </span>
   </div>

   <div
        style={{
     display: 'grid',
     gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
     gap: 14
    }}
   >
    {subjects.map((sub) => {
     let totalChapters = 0;
     let completedChapters = 0;

     sub.papers.forEach((p) => {
      p.chapters.forEach((c) => {
       totalChapters++;
       if (c.sections.every((s) => s.isCompleted)) {
        completedChapters++;
       }
      });
     });

     const percent = totalChapters > 0 ? Math.round((completedChapters / totalChapters) * 100) : 0;

     return (
      <div
       key={sub.id}
       className="glass-card"
       onClick={() => {
        sound.playTick();
        onOpenSubject(sub.name);
       }}
              style={{
        padding: '16px 18px',
        cursor: 'pointer',
        display: 'flex',
        flexDirection: 'column',
        gap: 12
       }}
      >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
         <div
                    style={{
           width: 32,
           height: 32,
           borderRadius: 8,
           background: 'rgba(255, 255, 255, 0.06)',
           display: 'flex',
           alignItems: 'center',
           justifyContent: 'center'
          }}
         >
          {getIcon(sub.name)}
         </div>
         <div>
                    <h4 style={{ margin: 0, fontSize: 15, fontWeight: 700, color: '#fff' }}>
           {sub.name}
          </h4>
                    <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>
           {completedChapters} of {totalChapters} Chapters Done
          </span>
         </div>
        </div>

        <div
                  style={{
          fontFamily: 'var(--font-mono)',
          fontSize: 16,
          fontWeight: 800,
          color: '#fff'
         }}
        >
         {percent}%
        </div>
       </div>

       {/* Progress Bar Track */}
       <div
                style={{
         width: '100%',
         height: 6,
         background: 'rgba(255, 255, 255, 0.08)',
         borderRadius: 'var(--radius-full)',
         overflow: 'hidden'
        }}
       >
        <div
                  style={{
          width: `${percent}%`,
          height: '100%',
          background: getGradient(sub.name),
          borderRadius: 'var(--radius-full)',
          transition: 'width 0.6s cubic-bezier(0.16, 1, 0.3, 1)'
         }}
        />
       </div>

       <div
                style={{
         display: 'flex',
         alignItems: 'center',
         justifyContent: 'flex-end',
         gap: 4,
         fontSize: 11.5,
         fontWeight: 600,
         color: 'var(--text-secondary)'
        }}
       >
        <span>View chapters</span>
        <ArrowRight size={12} />
       </div>
      </div>
     );
    })}
   </div>
  </div>
 );
};
