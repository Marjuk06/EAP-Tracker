import React from 'react';
import { ChevronDown, CheckCircle2, Circle, BookOpen, Play, FileText, Lightbulb } from 'lucide-react';
import { Chapter } from '../types';
import { sound } from '../services/audio';

interface ChapterExpandableItemProps {
 chapter: Chapter;
 isExpanded: boolean;
 onToggle: () => void;
 onSectionToggle: (sectionIndex: number) => void;
}

export const ChapterExpandableItem: React.FC<ChapterExpandableItemProps> = ({
 chapter,
 isExpanded,
 onToggle,
 onSectionToggle
}) => {
 const isAllDone = chapter.sections.every((s) => s.isCompleted);
 const completedCount = chapter.sections.filter((s) => s.isCompleted).length;

 const getSectionIcon = (name: string) => {
  switch (name) {
   case 'Book':
    return <BookOpen size={13} color='var(--accent-cyan)' />;
   case 'Slide':
    return <Play size={13} color='var(--accent-violet)' />;
   case 'QB':
    return <FileText size={13} color='var(--accent-amber)' />;
   case 'Concept':
    return <Lightbulb size={13} color='var(--accent-rose)' />;
   default:
    return null;
  }
 };

 return (
    <div style={{ marginBottom: 8 }}>
   {/* Split Button Header (Row with 2px gap) */}
   <div
        style={{
     display: 'flex',
     height: 56,
     alignItems: 'center',
     gap: 2,
     cursor: 'pointer'
    }}
    onClick={() => {
     sound.playTick();
     onToggle();
    }}
   >
    {/* Left: Chapter Title Box */}
    <div
     className="haze-card"
          style={{
      flex: 1,
      height: '100%',
      borderRadius: '28px 4px 4px 28px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 20px',
      borderColor: isAllDone ? 'rgba(129, 199, 132, 0.4)' : 'rgba(255, 255, 255, 0.12)'
     }}
    >
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
      <span
              style={{
        fontSize: 15,
        fontWeight: 700,
        color: isAllDone ? '#81c784' : '#fff'
       }}
      >
       {chapter.name}
      </span>
     </div>

     <span
            style={{
       fontSize: 11.5,
       fontWeight: 700,
       color: isAllDone ? '#81c784' : 'rgba(255, 255, 255, 0.5)'
      }}
     >
      {completedCount}/4
     </span>
    </div>

    {/* Right: Dropdown Arrow Box */}
    <div
     className="haze-card"
          style={{
      width: 56,
      height: '100%',
      borderRadius: isExpanded ? '4px 28px 28px 4px' : '4px 28px 28px 4px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      color: 'rgba(230, 224, 233, 0.7)'
     }}
    >
     <div
            style={{
       transform: isExpanded ? 'rotate(180deg)' : 'rotate(0deg)',
       transition: 'transform 0.25s cubic-bezier(0.16, 1, 0.3, 1)'
      }}
     >
      <ChevronDown size={20} />
     </div>
    </div>
   </div>

   {/* Expanded 4 Milestone Checkboxes */}
   {isExpanded && (
    <div
          style={{
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))',
      gap: 6,
      marginTop: 6,
      padding: '8px 10px',
      background: 'rgba(255, 255, 255, 0.03)',
      borderRadius: '16px',
      border: '0.5px solid rgba(255, 255, 255, 0.08)'
     }}
    >
     {chapter.sections.map((sec, secIdx) => (
      <button
       key={sec.name}
       onClick={() => {
        onSectionToggle(secIdx);
       }}
              style={{
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        padding: '10px 12px',
        borderRadius: '12px',
        border: sec.isCompleted
         ? '0.5px solid rgba(129, 199, 132, 0.5)'
         : '0.5px solid rgba(255, 255, 255, 0.1)',
        background: sec.isCompleted
         ? 'rgba(129, 199, 132, 0.15)'
         : 'rgba(255, 255, 255, 0.04)',
        color: sec.isCompleted ? '#fff' : 'rgba(255, 255, 255, 0.75)',
        fontSize: 12.5,
        fontWeight: 600,
        cursor: 'pointer',
        transition: 'all 0.18s ease'
       }}
      >
       {sec.isCompleted ? (
        <CheckCircle2 size={16} color="#81c784" />
       ) : (
        <Circle size={16} color="rgba(255, 255, 255, 0.4)" />
       )}
              <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
        {getSectionIcon(sec.name)}
        <span>{sec.name}</span>
       </div>
      </button>
     ))}
    </div>
   )}
  </div>
 );
};
