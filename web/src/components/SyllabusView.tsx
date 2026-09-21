import React, { useState } from 'react';
import {
 Zap,
 FlaskConical,
 Calculator,
 Dna,
 Search,
 CheckCircle2,
 Circle,
 BookOpen,
 Cast,
 Landmark,
 Lightbulb,
 ChevronDown,
 ChevronUp,
 Sparkles
} from 'lucide-react';
import confetti from 'canvas-confetti';
import { Subject, AdmissionTrack } from '../types';
import { storage } from '../services/storage';
import { sound } from '../services/audio';

interface SyllabusViewProps {
 subjects: Subject[];
 initialSubject?: string;
 userTrack: AdmissionTrack;
 onTrackChange: (track: AdmissionTrack) => void;
}

export const SyllabusView: React.FC<SyllabusViewProps> = ({
 subjects,
 initialSubject,
 userTrack,
 onTrackChange
}) => {
 const [selectedSubjectName, setSelectedSubjectName] = useState<string>(initialSubject || 'Physics');
 const [selectedPaperIndex, setSelectedPaperIndex] = useState<number>(0);
 const [searchQuery, setSearchQuery] = useState<string>('');
 const [statusFilter, setStatusFilter] = useState<'all' | 'pending' | 'completed' | 'in_progress'>('all');
 const [expandedChapterIds, setExpandedChapterIds] = useState<Set<string>>(new Set());

 // Filter available subjects based on user track
 const filteredSubjects = subjects.filter((s) => {
  if (userTrack === 'engineering') {
   return s.name !== 'Biology';
  }
  if (userTrack === 'medical') {
   return s.name !== 'Higher Math';
  }
  return true; // Varsity A has all
 });

 const currentSubject = filteredSubjects.find((s) => s.name === selectedSubjectName) || filteredSubjects[0] || subjects[0];
 const currentPaper = currentSubject?.papers[selectedPaperIndex] || currentSubject?.papers[0];

 const toggleAccordion = (chapId: string) => {
  sound.playTick();
  setExpandedChapterIds((prev) => {
   const next = new Set(prev);
   if (next.has(chapId)) {
    next.delete(chapId);
   } else {
    next.add(chapId);
   }
   return next;
  });
 };

 const handleSectionToggle = (
  subjectName: string,
  paperName: string,
  chapterName: string,
  sectionName: string,
  currentStatus: boolean,
  allSections: { name: string; isCompleted: boolean }[]
 ) => {
  const newStatus = !currentStatus;
  storage.saveSyllabusSection(subjectName, paperName, chapterName, sectionName, newStatus);

  if (newStatus) {
   sound.playSuccess();
   // Check if all other sections are completed
   const otherSectionsDone = allSections
    .filter((s) => s.name !== sectionName)
    .every((s) => s.isCompleted);

   if (otherSectionsDone) {
    // Confetti Celebration!
    confetti({
     particleCount: 50,
     spread: 60,
     origin: { y: 0.8 }
    });
   }
  } else {
   sound.playTick();
  }
 };

 // Chapter filtering
 const visibleChapters = currentPaper?.chapters.filter((chap) => {
  const matchesSearch = chap.name.toLowerCase().includes(searchQuery.toLowerCase());
  if (!matchesSearch) return false;

  const completedCount = chap.sections.filter((s) => s.isCompleted).length;
  const isCompleted = completedCount === chap.sections.length;
  const isInProgress = completedCount > 0 && completedCount < chap.sections.length;
  const isPending = completedCount === 0;

  if (statusFilter === 'completed') return isCompleted;
  if (statusFilter === 'in_progress') return isInProgress;
  if (statusFilter === 'pending') return isPending;
  return true;
 }) || [];

 const getSubjectIcon = (name: string) => {
  switch (name.toLowerCase()) {
   case 'physics':
    return <Zap size={16} />;
   case 'chemistry':
    return <FlaskConical size={16} />;
   case 'higher math':
    return <Calculator size={16} />;
   case 'biology':
    return <Dna size={16} />;
   default:
    return <Zap size={16} />;
  }
 };

 const getSectionIcon = (secName: string) => {
  switch (secName) {
   case 'Book':
    return <BookOpen size={13} />;
   case 'Slide':
    return <Cast size={13} />;
   case 'QB':
    return <Landmark size={13} />;
   case 'Concept':
    return <Lightbulb size={13} />;
   default:
    return <BookOpen size={13} />;
  }
 };

 return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
   {/* Top Header Card with Track Switcher */}
      <div className="glass-card" style={{ padding: '20px 24px' }}>
    <div
          style={{
      display: 'flex',
      flexWrap: 'wrap',
      alignItems: 'center',
      justifyContent: 'space-between',
      gap: 16
     }}
    >
     <div>
            <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--accent-cyan)', letterSpacing: 1 }}>
       ADMISSION PROGRAM SYLLABUS
      </div>
            <h2 style={{ margin: '4px 0 0 0', fontSize: 22, fontWeight: 800, color: '#fff' }}>
       Master Syllabus Tracker
      </h2>
     </div>

     {/* Track Switcher */}
     <div
            style={{
       display: 'flex',
       alignItems: 'center',
       gap: 6,
       background: 'rgba(255, 255, 255, 0.05)',
       padding: '4px',
       borderRadius: 'var(--radius-full)',
       border: '1px solid var(--border-glass)'
      }}
     >
      {[
       { id: 'engineering' as AdmissionTrack, label: 'Engineering (PCM)' },
       { id: 'medical' as AdmissionTrack, label: 'Medical (PCB)' },
       { id: 'varsity_a' as AdmissionTrack, label: "Varsity 'KA' (All)" }
      ].map((t) => (
       <button
        key={t.id}
        onClick={() => {
         sound.playTick();
         onTrackChange(t.id);
        }}
                style={{
         padding: '6px 14px',
         borderRadius: 'var(--radius-full)',
         border: 'none',
         background: userTrack === t.id ? 'var(--accent-cyan)' : 'transparent',
         color: userTrack === t.id ? '#06101e' : 'var(--text-secondary)',
         fontSize: 12,
         fontWeight: userTrack === t.id ? 700 : 500,
         cursor: 'pointer',
         transition: 'all 0.2s ease'
        }}
       >
        {t.label}
       </button>
      ))}
     </div>
    </div>

    {/* Subject Tabs */}
    <div
          style={{
      display: 'flex',
      gap: 8,
      overflowX: 'auto',
      marginTop: 18,
      paddingBottom: 4
     }}
    >
     {filteredSubjects.map((sub) => {
      const isSelected = sub.name === currentSubject.name;
      return (
       <button
        key={sub.id}
        onClick={() => {
         sound.playTick();
         setSelectedSubjectName(sub.name);
         setSelectedPaperIndex(0);
        }}
                style={{
         display: 'flex',
         alignItems: 'center',
         gap: 8,
         padding: '10px 18px',
         borderRadius: 'var(--radius-md)',
         border: isSelected ? '1px solid var(--accent-cyan)' : '1px solid var(--border-glass)',
         background: isSelected ? 'rgba(0, 229, 255, 0.15)' : 'rgba(255, 255, 255, 0.04)',
         color: isSelected ? '#fff' : 'var(--text-secondary)',
         fontWeight: isSelected ? 700 : 500,
         fontSize: 14,
         cursor: 'pointer',
         boxShadow: isSelected ? '0 0 16px rgba(0, 229, 255, 0.2)' : 'none',
         transition: 'all 0.2s ease'
        }}
       >
        {getSubjectIcon(sub.name)}
        <span>{sub.name}</span>
       </button>
      );
     })}
    </div>
   </div>

   {/* Paper Switcher & Search Bar Row */}
   <div
        style={{
     display: 'flex',
     flexWrap: 'wrap',
     alignItems: 'center',
     justifyContent: 'space-between',
     gap: 12
    }}
   >
    {/* 1st Paper / 2nd Paper Tabs */}
        <div style={{ display: 'flex', gap: 8 }}>
     {currentSubject.papers.map((p, idx) => {
      const isSelected = selectedPaperIndex === idx;
      const completedChaps = p.chapters.filter((c) => c.sections.every((s) => s.isCompleted)).length;
      return (
       <button
        key={p.id}
        onClick={() => {
         sound.playTick();
         setSelectedPaperIndex(idx);
        }}
                style={{
         display: 'flex',
         alignItems: 'center',
         gap: 8,
         padding: '8px 16px',
         borderRadius: 'var(--radius-sm)',
         border: isSelected ? '1px solid var(--accent-violet)' : '1px solid var(--border-glass)',
         background: isSelected ? 'rgba(139, 92, 246, 0.2)' : 'rgba(255, 255, 255, 0.04)',
         color: isSelected ? '#fff' : 'var(--text-secondary)',
         fontWeight: isSelected ? 700 : 500,
         fontSize: 13,
         cursor: 'pointer'
        }}
       >
        <span>{p.name}</span>
        <span
                  style={{
          fontSize: 11,
          padding: '2px 6px',
          borderRadius: 4,
          background: 'rgba(255, 255, 255, 0.1)',
          color: isSelected ? '#fff' : 'var(--text-muted)'
         }}
        >
         {completedChaps}/{p.chapters.length}
        </span>
       </button>
      );
     })}
    </div>

    {/* Search and Filters */}
        <div style={{ display: 'flex', gap: 10, flex: 1, minWidth: 260, justifyContent: 'flex-end' }}>
          <div style={{ position: 'relative', width: '100%', maxWidth: 260 }}>
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
       placeholder="Search chapters..."
       value={searchQuery}
       onChange={(e) => setSearchQuery(e.target.value)}
              style={{
        width: '100%',
        paddingLeft: 34,
        paddingRight: 12,
        paddingTop: 8,
        paddingBottom: 8,
        fontSize: 13
       }}
      />
     </div>

     <select
      value={statusFilter}
      onChange={(e) => setStatusFilter(e.target.value as any)}
            style={{ padding: '8px 12px', fontSize: 13 }}
     >
      <option value="all">All Status</option>
      <option value="pending">Pending</option>
      <option value="in_progress">In Progress</option>
      <option value="completed">Completed</option>
     </select>
    </div>
   </div>

   {/* Chapters Accordion List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
    {visibleChapters.length === 0 ? (
     <div
      className="glass-card"
            style={{ padding: 36, textAlign: 'center', color: 'var(--text-muted)' }}
     >
      No chapters found matching "{searchQuery}"
     </div>
    ) : (
     visibleChapters.map((chap, idx) => {
      const completedCount = chap.sections.filter((s) => s.isCompleted).length;
      const isAllDone = completedCount === chap.sections.length;
      const isExpanded = expandedChapterIds.has(chap.id);
      const percent = Math.round((completedCount / chap.sections.length) * 100);

      return (
       <div
        key={chap.id}
        className="glass-card"
                style={{
         padding: '16px 20px',
         borderColor: isAllDone ? 'rgba(16, 185, 129, 0.4)' : 'var(--border-glass)'
        }}
       >
        {/* Chapter Header Row */}
        <div
         onClick={() => toggleAccordion(chap.id)}
                  style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          cursor: 'pointer',
          userSelect: 'none'
         }}
        >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12, flex: 1 }}>
          <div
                      style={{
            width: 28,
            height: 28,
            borderRadius: 8,
            background: isAllDone ? 'rgba(16, 185, 129, 0.2)' : 'rgba(255, 255, 255, 0.06)',
            color: isAllDone ? 'var(--accent-emerald)' : 'var(--text-muted)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 12,
            fontWeight: 700
           }}
          >
           {idx + 1}
          </div>

          <div>
           <h4
                        style={{
             margin: 0,
             fontSize: 16,
             fontWeight: 700,
             color: isAllDone ? '#10b981' : '#fff'
            }}
           >
            {chap.name}
           </h4>
                      <div style={{ fontSize: 11.5, color: 'var(--text-muted)', marginTop: 2 }}>
            {completedCount}/4 Milestones Completed ({percent}%)
           </div>
          </div>
         </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          {isAllDone && (
           <span
                        style={{
             fontSize: 11,
             fontWeight: 700,
             padding: '2px 8px',
             borderRadius: 'var(--radius-full)',
             background: 'rgba(16, 185, 129, 0.15)',
             color: 'var(--accent-emerald)',
             border: '1px solid rgba(16, 185, 129, 0.3)',
             display: 'flex',
             alignItems: 'center',
             gap: 4
            }}
           >
            <Sparkles size={11} />
            Done
           </span>
          )}

                    <div style={{ color: 'var(--text-muted)' }}>
           {isExpanded ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
          </div>
         </div>
        </div>

        {/* Progress bar inside card */}
        <div
                  style={{
          width: '100%',
          height: 4,
          background: 'rgba(255, 255, 255, 0.06)',
          borderRadius: 'var(--radius-full)',
          marginTop: 12,
          overflow: 'hidden'
         }}
        >
         <div
                    style={{
           width: `${percent}%`,
           height: '100%',
           background: isAllDone
            ? 'var(--accent-emerald)'
            : 'linear-gradient(90deg, var(--accent-cyan), var(--accent-violet))',
           borderRadius: 'var(--radius-full)',
           transition: 'width 0.4s ease'
          }}
         />
        </div>

        {/* 4 Study Milestones Buttons (Expandable or Grid) */}
        <div
                  style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))',
          gap: 8,
          marginTop: 14
         }}
        >
         {chap.sections.map((sec) => (
          <button
           key={sec.name}
           onClick={() =>
            handleSectionToggle(
             currentSubject.name,
             currentPaper.name,
             chap.name,
             sec.name,
             sec.isCompleted,
             chap.sections
            )
           }
                      style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            padding: '10px 12px',
            borderRadius: 'var(--radius-sm)',
            border: sec.isCompleted
             ? '1px solid rgba(16, 185, 129, 0.5)'
             : '1px solid var(--border-glass)',
            background: sec.isCompleted
             ? 'rgba(16, 185, 129, 0.16)'
             : 'rgba(255, 255, 255, 0.03)',
            color: sec.isCompleted ? '#fff' : 'var(--text-secondary)',
            fontSize: 12.5,
            fontWeight: 600,
            cursor: 'pointer',
            transition: 'all 0.18s ease'
           }}
          >
           {sec.isCompleted ? (
            <CheckCircle2 size={16} color="var(--accent-emerald)" />
           ) : (
            <Circle size={16} color="var(--text-muted)" />
           )}
                      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            {getSectionIcon(sec.name)}
            <span>{sec.label}</span>
           </div>
          </button>
         ))}
        </div>
       </div>
      );
     })
    )}
   </div>
  </div>
 );
};
