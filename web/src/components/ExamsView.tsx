import React, { useState } from 'react';
import {
 Award,
 Plus,
 Trash2,
 Edit2,
 TrendingUp,
 BarChart2,
 Calendar,
 AlertCircle
} from 'lucide-react';
import { ExamRecord } from '../types';
import { storage } from '../services/storage';
import { sound } from '../services/audio';

interface ExamsViewProps {
 exams: ExamRecord[];
}

export const ExamsView: React.FC<ExamsViewProps> = ({ exams }) => {
 const [showModal, setShowModal] = useState(false);
 const [editingId, setEditingId] = useState<string | null>(null);

 // Form states
 const [title, setTitle] = useState('');
 const [examType, setExamType] = useState<ExamRecord['examType']>('Weekly');
 const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
 const [physicsMarks, setPhysicsMarks] = useState('');
 const [chemistryMarks, setChemistryMarks] = useState('');
 const [mathMarks, setMathMarks] = useState('');
 const [biologyMarks, setBiologyMarks] = useState('');
 const [totalMarks, setTotalMarks] = useState('100');
 const [highestMarks, setHighestMarks] = useState('');
 const [meritPosition, setMeritPosition] = useState('');
 const [negativeMarks, setNegativeMarks] = useState('0');
 const [notes, setNotes] = useState('');

 const openAddModal = () => {
  sound.playTick();
  setEditingId(null);
  setTitle('');
  setExamType('Weekly');
  setDate(new Date().toISOString().split('T')[0]);
  setPhysicsMarks('');
  setChemistryMarks('');
  setMathMarks('');
  setBiologyMarks('');
  setTotalMarks('100');
  setHighestMarks('');
  setMeritPosition('');
  setNegativeMarks('0');
  setNotes('');
  setShowModal(true);
 };

 const openEditModal = (exam: ExamRecord) => {
  sound.playTick();
  setEditingId(exam.id);
  setTitle(exam.title);
  setExamType(exam.examType);
  setDate(exam.date);
  setPhysicsMarks(exam.physicsMarks !== undefined ? String(exam.physicsMarks) : '');
  setChemistryMarks(exam.chemistryMarks !== undefined ? String(exam.chemistryMarks) : '');
  setMathMarks(exam.mathMarks !== undefined ? String(exam.mathMarks) : '');
  setBiologyMarks(exam.biologyMarks !== undefined ? String(exam.biologyMarks) : '');
  setTotalMarks(String(exam.totalMarks));
  setHighestMarks(exam.highestMarks !== undefined ? String(exam.highestMarks) : '');
  setMeritPosition(exam.meritPosition !== undefined ? String(exam.meritPosition) : '');
  setNegativeMarks(exam.negativeMarks !== undefined ? String(exam.negativeMarks) : '0');
  setNotes(exam.notes || '');
  setShowModal(true);
 };

 const handleSubmit = (e: React.FormEvent) => {
  e.preventDefault();

  const p = parseFloat(physicsMarks) || 0;
  const c = parseFloat(chemistryMarks) || 0;
  const m = parseFloat(mathMarks) || 0;
  const b = parseFloat(biologyMarks) || 0;
  const neg = parseFloat(negativeMarks) || 0;
  const tot = parseFloat(totalMarks) || 100;
  const high = highestMarks ? parseFloat(highestMarks) : undefined;
  const rank = meritPosition ? parseInt(meritPosition, 10) : undefined;

  const obtained = Math.max(0, p + c + m + b - neg);

  storage.saveExam({
   id: editingId || undefined,
   title: title || `${examType} Exam`,
   examType,
   date,
   physicsMarks: physicsMarks ? p : undefined,
   chemistryMarks: chemistryMarks ? c : undefined,
   mathMarks: mathMarks ? m : undefined,
   biologyMarks: biologyMarks ? b : undefined,
   totalMarks: tot,
   obtainedMarks: obtained,
   highestMarks: high,
   meritPosition: rank,
   negativeMarks: neg > 0 ? neg : undefined,
   notes: notes || undefined
  });

  sound.playSuccess();
  setShowModal(false);
 };

 const handleDelete = (id: string) => {
  sound.playTick();
  if (window.confirm('Delete this exam record?')) {
   storage.deleteExam(id);
  }
 };

 // Performance calculations
 const totalExams = exams.length;
 const avgPercent = totalExams > 0
  ? Math.round(exams.reduce((acc, e) => acc + (e.obtainedMarks / e.totalMarks) * 100, 0) / totalExams)
  : 0;

 const highestRecordedPercent = totalExams > 0
  ? Math.round(Math.max(...exams.map((e) => (e.obtainedMarks / e.totalMarks) * 100)))
  : 0;

 return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
   {/* Top Banner */}
      <div className="glass-card" style={{ padding: '20px 24px' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: 16 }}>
     <div>
            <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--accent-cyan)', letterSpacing: 1 }}>
       ADMISSION MARKS & ACCURACY
      </div>
            <h2 style={{ margin: '4px 0 0 0', fontSize: 22, fontWeight: 800, color: '#fff' }}>
       Exam Records & Analytics
      </h2>
     </div>

     <button onClick={openAddModal} className="btn-primary">
      <Plus size={16} />
      <span>Add New Exam</span>
     </button>
    </div>

    {/* Analytics Summary Stats */}
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
            <div style={{ fontSize: 11, color: 'var(--text-muted)', fontWeight: 600 }}>TOTAL EXAMS</div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 24, fontWeight: 800, color: '#fff', marginTop: 4 }}>
       {totalExams}
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
            <div style={{ fontSize: 11, color: 'var(--accent-cyan)', fontWeight: 600 }}>AVERAGE SCORE</div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 24, fontWeight: 800, color: 'var(--accent-cyan)', marginTop: 4 }}>
       {avgPercent}%
      </div>
     </div>

     <div
            style={{
       padding: '14px 16px',
       borderRadius: 'var(--radius-md)',
       background: 'rgba(16, 185, 129, 0.08)',
       border: '1px solid rgba(16, 185, 129, 0.25)'
      }}
     >
            <div style={{ fontSize: 11, color: 'var(--accent-emerald)', fontWeight: 600 }}>HIGHEST SCORE</div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 24, fontWeight: 800, color: 'var(--accent-emerald)', marginTop: 4 }}>
       {highestRecordedPercent}%
      </div>
     </div>
    </div>
   </div>

   {/* Performance Graph Card */}
   {exams.length > 0 && (
        <div className="glass-card" style={{ padding: '20px 24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
            <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: '#fff', display: 'flex', alignItems: 'center', gap: 8 }}>
       <TrendingUp size={18} color="var(--accent-cyan)" />
       <span>Score Trajectory Graph</span>
      </h3>
            <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>Latest Exams</span>
     </div>

          <div style={{ display: 'flex', alignItems: 'flex-end', gap: 10, height: 140, paddingTop: 20 }}>
      {exams.slice(0, 10).reverse().map((ex) => {
       const p = Math.round((ex.obtainedMarks / ex.totalMarks) * 100);
       return (
        <div
         key={ex.id}
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
                  <span style={{ fontSize: 10, fontWeight: 700, color: 'var(--text-muted)' }}>
          {p}%
         </span>
         <div
                    style={{
           width: '100%',
           maxWidth: 32,
           height: `${Math.max(8, p)}%`,
           borderRadius: '6px 6px 0 0',
           background: p >= 80
            ? 'var(--accent-emerald)'
            : p >= 60
            ? 'var(--accent-cyan)'
            : 'var(--accent-amber)',
           boxShadow: '0 0 10px rgba(0, 229, 255, 0.2)'
          }}
         />
         <span
                    style={{
           fontSize: 9.5,
           color: 'var(--text-muted)',
           whiteSpace: 'nowrap',
           overflow: 'hidden',
           textOverflow: 'ellipsis',
           maxWidth: 40
          }}
         >
          {ex.date.slice(5)}
         </span>
        </div>
       );
      })}
     </div>
    </div>
   )}

   {/* Exam Records List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <h3 style={{ margin: '8px 0 0 0', fontSize: 17, fontWeight: 700, color: '#fff' }}>
     Recorded Exam Logs
    </h3>

    {exams.length === 0 ? (
          <div className="glass-card" style={{ padding: 40, textAlign: 'center', color: 'var(--text-muted)' }}>
            <Award size={36} style={{ margin: '0 auto 12px auto', opacity: 0.4 }} />
      <div>No exams logged yet. Click "Add New Exam" to record your scores!</div>
     </div>
    ) : (
     exams.map((ex) => {
      const percent = Math.round((ex.obtainedMarks / ex.totalMarks) * 100);

      return (
              <div key={ex.id} className="glass-card" style={{ padding: '18px 22px' }}>
                <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div
                      style={{
            padding: '6px 10px',
            borderRadius: 'var(--radius-sm)',
            background: 'rgba(255, 255, 255, 0.06)',
            border: '1px solid var(--border-glass)',
            fontSize: 12,
            fontWeight: 700,
            color: 'var(--accent-cyan)'
           }}
          >
           {ex.examType}
          </div>

          <div>
                      <h4 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: '#fff' }}>
            {ex.title}
           </h4>
                      <div style={{ fontSize: 11.5, color: 'var(--text-muted)', marginTop: 2, display: 'flex', gap: 10 }}>
            <span>{ex.date}</span>
            {ex.meritPosition && <span>• Merit: #{ex.meritPosition}</span>}
            {ex.highestMarks && <span>• Highest: {ex.highestMarks}</span>}
           </div>
          </div>
         </div>

         {/* Marks Display & Actions */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                    <div style={{ textAlign: 'right' }}>
                      <div style={{ fontFamily: 'var(--font-mono)', fontSize: 20, fontWeight: 800, color: '#fff' }}>
                        {ex.obtainedMarks} <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>/ {ex.totalMarks}</span>
           </div>
                      <div style={{ fontSize: 11.5, fontWeight: 700, color: percent >= 75 ? 'var(--accent-emerald)' : 'var(--accent-cyan)' }}>
            {percent}% Score
           </div>
          </div>

                    <div style={{ display: 'flex', gap: 6 }}>
           <button
            onClick={() => openEditModal(ex)}
            className="btn-glass"
                        style={{ padding: 6 }}
            title="Edit"
           >
            <Edit2 size={15} />
           </button>
           <button
            onClick={() => handleDelete(ex.id)}
            className="btn-glass"
                        style={{ padding: 6, color: 'var(--accent-rose)' }}
            title="Delete"
           >
            <Trash2 size={15} />
           </button>
          </div>
         </div>
        </div>

        {/* Subject Breakdown Badges */}
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 12 }}>
         {ex.physicsMarks !== undefined && (
                    <span style={{ fontSize: 11, padding: '3px 8px', borderRadius: 6, background: 'rgba(0, 229, 255, 0.1)', color: 'var(--accent-cyan)' }}>
           Physics: {ex.physicsMarks}
          </span>
         )}
         {ex.chemistryMarks !== undefined && (
                    <span style={{ fontSize: 11, padding: '3px 8px', borderRadius: 6, background: 'rgba(16, 185, 129, 0.1)', color: 'var(--accent-emerald)' }}>
           Chemistry: {ex.chemistryMarks}
          </span>
         )}
         {ex.mathMarks !== undefined && (
                    <span style={{ fontSize: 11, padding: '3px 8px', borderRadius: 6, background: 'rgba(139, 92, 246, 0.1)', color: 'var(--accent-violet)' }}>
           Math: {ex.mathMarks}
          </span>
         )}
         {ex.biologyMarks !== undefined && (
                    <span style={{ fontSize: 11, padding: '3px 8px', borderRadius: 6, background: 'rgba(244, 63, 94, 0.1)', color: 'var(--accent-rose)' }}>
           Biology: {ex.biologyMarks}
          </span>
         )}
         {ex.negativeMarks !== undefined && ex.negativeMarks > 0 && (
                    <span style={{ fontSize: 11, padding: '3px 8px', borderRadius: 6, background: 'rgba(245, 158, 11, 0.1)', color: 'var(--accent-amber)' }}>
           Negative: -{ex.negativeMarks}
          </span>
         )}
        </div>
       </div>
      );
     })
    )}
   </div>

   {/* Add / Edit Exam Modal */}
   {showModal && (
    <div
          style={{
      position: 'fixed',
      inset: 0,
      zIndex: 100,
      background: 'rgba(0, 0, 0, 0.78)',
      backdropFilter: 'blur(12px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: 20
     }}
     onClick={() => setShowModal(false)}
    >
     <div
      className="glass-modal"
            style={{ width: '100%', maxWidth: 480, maxHeight: '90vh', overflowY: 'auto', padding: 24 }}
      onClick={(e) => e.stopPropagation()}
     >
            <h3 style={{ margin: '0 0 16px 0', fontSize: 19, color: '#fff' }}>
       {editingId ? 'Edit Exam Record' : 'Add New Exam Score'}
      </h3>

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
       <div>
                <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>
         EXAM TITLE
        </label>
        <input
         type="text"
         placeholder="e.g. Weekly Exam-01 (P1+C1+M1)"
         value={title}
         onChange={(e) => setTitle(e.target.value)}
                  style={{ width: '100%' }}
         required
        />
       </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        <div>
                  <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>
          TYPE
         </label>
         <select
          value={examType}
          onChange={(e) => setExamType(e.target.value as any)}
                    style={{ width: '100%' }}
         >
          <option value="Daily">Daily Exam</option>
          <option value="Weekly">Weekly Exam</option>
          <option value="Model Test">Model Test</option>
          <option value="Grand Final">Grand Final</option>
          <option value="College">College Exam</option>
         </select>
        </div>

        <div>
                  <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>
          DATE
         </label>
         <input
          type="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
                    style={{ width: '100%' }}
          required
         />
        </div>
       </div>

       {/* Subject Wise Marks */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        <div>
                  <label style={{ display: 'block', fontSize: 12, color: 'var(--accent-cyan)', marginBottom: 4 }}>
          PHYSICS MARKS
         </label>
         <input
          type="number"
          step="0.25"
          placeholder="e.g. 28"
          value={physicsMarks}
          onChange={(e) => setPhysicsMarks(e.target.value)}
                    style={{ width: '100%' }}
         />
        </div>

        <div>
                  <label style={{ display: 'block', fontSize: 12, color: 'var(--accent-emerald)', marginBottom: 4 }}>
          CHEMISTRY MARKS
         </label>
         <input
          type="number"
          step="0.25"
          placeholder="e.g. 26"
          value={chemistryMarks}
          onChange={(e) => setChemistryMarks(e.target.value)}
                    style={{ width: '100%' }}
         />
        </div>

        <div>
                  <label style={{ display: 'block', fontSize: 12, color: 'var(--accent-violet)', marginBottom: 4 }}>
          MATH MARKS
         </label>
         <input
          type="number"
          step="0.25"
          placeholder="e.g. 30"
          value={mathMarks}
          onChange={(e) => setMathMarks(e.target.value)}
                    style={{ width: '100%' }}
         />
        </div>

        <div>
                  <label style={{ display: 'block', fontSize: 12, color: 'var(--accent-rose)', marginBottom: 4 }}>
          BIOLOGY / GK
         </label>
         <input
          type="number"
          step="0.25"
          placeholder="e.g. 20"
          value={biologyMarks}
          onChange={(e) => setBiologyMarks(e.target.value)}
                    style={{ width: '100%' }}
         />
        </div>
       </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10 }}>
        <div>
                  <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>
          TOTAL MARKS
         </label>
         <input
          type="number"
          placeholder="100"
          value={totalMarks}
          onChange={(e) => setTotalMarks(e.target.value)}
                    style={{ width: '100%' }}
          required
         />
        </div>

        <div>
                  <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>
          HIGHEST
         </label>
         <input
          type="number"
          placeholder="e.g. 95"
          value={highestMarks}
          onChange={(e) => setHighestMarks(e.target.value)}
                    style={{ width: '100%' }}
         />
        </div>

        <div>
                  <label style={{ display: 'block', fontSize: 12, color: 'var(--text-muted)', marginBottom: 4 }}>
          MERIT RANK
         </label>
         <input
          type="number"
          placeholder="e.g. 14"
          value={meritPosition}
          onChange={(e) => setMeritPosition(e.target.value)}
                    style={{ width: '100%' }}
         />
        </div>
       </div>

       <div>
                <label style={{ display: 'block', fontSize: 12, color: 'var(--accent-amber)', marginBottom: 4 }}>
         NEGATIVE MARKS DEDUCTED
        </label>
        <input
         type="number"
         step="0.25"
         placeholder="0.0"
         value={negativeMarks}
         onChange={(e) => setNegativeMarks(e.target.value)}
                  style={{ width: '100%' }}
        />
       </div>

              <div style={{ display: 'flex', gap: 10, marginTop: 14 }}>
        <button
         type="button"
         onClick={() => setShowModal(false)}
         className="btn-glass"
                  style={{ flex: 1 }}
        >
         Cancel
        </button>
                <button type="submit" className="btn-primary" style={{ flex: 1 }}>
         Save Record
        </button>
       </div>
      </form>
     </div>
    </div>
   )}
  </div>
 );
};
