import React, { useState } from 'react';
import { X, Check } from 'lucide-react';
import { sound } from '../services/audio';
import { storage } from '../services/storage';

interface ExamMarksDialogProps {
  date: string;
  day: string;
  examName: string;
  onDismiss: () => void;
}

export const ExamMarksDialog: React.FC<ExamMarksDialogProps> = ({
  date,
  day,
  examName,
  onDismiss
}) => {
  // Check if existing record exists
  const existingExams = storage.getExams();
  const existing = existingExams.find((e) => e.title === examName || (e.date === date && e.title.includes(examName.slice(0, 8))));

  const [mcqMarks, setMcqMarks] = useState<string>(existing?.physicsMarks !== undefined ? String(existing.physicsMarks) : '');
  const [maxMcq, setMaxMcq] = useState<string>('20');
  const [writtenMarks, setWrittenMarks] = useState<string>(existing?.chemistryMarks !== undefined ? String(existing.chemistryMarks) : '');
  const [maxWritten, setMaxWritten] = useState<string>('10');
  const [negMarks, setNegMarks] = useState<string>(existing?.negativeMarks !== undefined ? String(existing.negativeMarks) : '0');

  const mcq = parseFloat(mcqMarks) || 0;
  const written = parseFloat(writtenMarks) || 0;
  const neg = parseFloat(negMarks) || 0;
  const totalMax = (parseFloat(maxMcq) || 0) + (parseFloat(maxWritten) || 0);
  const totalObtained = Math.max(0, mcq + written - neg);
  const percent = totalMax > 0 ? Math.round((totalObtained / totalMax) * 100) : 0;

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    sound.playSuccess();

    storage.saveExam({
      id: existing?.id,
      title: examName,
      examType: examName.toLowerCase().includes('weekly') ? 'Weekly' : 'Daily',
      date: date || new Date().toISOString().split('T')[0],
      physicsMarks: mcq,
      chemistryMarks: written,
      totalMarks: totalMax > 0 ? totalMax : 100,
      obtainedMarks: totalObtained,
      negativeMarks: neg > 0 ? neg : undefined
    });

    onDismiss();
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 150,
        background: 'rgba(0, 0, 0, 0.75)',
        backdropFilter: 'blur(16px)',
        WebkitBackdropFilter: 'blur(16px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 20
      }}
      onClick={onDismiss}
    >
      <div
        className="glass-dialog"
        style={{
          width: '100%',
          maxWidth: 400,
          padding: '24px 22px'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <div>
            <div style={{ fontSize: 11, fontWeight: 800, color: '#d0bcff', letterSpacing: '1px' }}>
              EXAM SCORE LOG
            </div>
            <h3 style={{ margin: '2px 0 0 0', fontSize: 16, fontWeight: 700, color: '#fff' }}>
              {examName}
            </h3>
            <div style={{ fontSize: 11.5, color: 'rgba(255,255,255,0.5)', marginTop: 2 }}>
              {date} • {day}
            </div>
          </div>

          <button
            onClick={onDismiss}
            style={{
              width: 32,
              height: 32,
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
            <X size={16} />
          </button>
        </div>

        <form onSubmit={handleSave} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          {/* MCQ Inputs */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            <div>
              <label style={{ display: 'block', fontSize: 11, color: '#d0bcff', fontWeight: 700, marginBottom: 4 }}>
                MCQ OBTAINED
              </label>
              <input
                type="number"
                step="0.25"
                placeholder="0"
                value={mcqMarks}
                onChange={(e) => setMcqMarks(e.target.value)}
                style={{ width: '100%' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: 11, color: 'rgba(255,255,255,0.5)', fontWeight: 600, marginBottom: 4 }}>
                MAX MCQ
              </label>
              <input
                type="number"
                placeholder="20"
                value={maxMcq}
                onChange={(e) => setMaxMcq(e.target.value)}
                style={{ width: '100%' }}
              />
            </div>
          </div>

          {/* Written Inputs */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            <div>
              <label style={{ display: 'block', fontSize: 11, color: '#81c784', fontWeight: 700, marginBottom: 4 }}>
                WRITTEN OBTAINED
              </label>
              <input
                type="number"
                step="0.25"
                placeholder="0"
                value={writtenMarks}
                onChange={(e) => setWrittenMarks(e.target.value)}
                style={{ width: '100%' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: 11, color: 'rgba(255,255,255,0.5)', fontWeight: 600, marginBottom: 4 }}>
                MAX WRITTEN
              </label>
              <input
                type="number"
                placeholder="10"
                value={maxWritten}
                onChange={(e) => setMaxWritten(e.target.value)}
                style={{ width: '100%' }}
              />
            </div>
          </div>

          {/* Negative Marks */}
          <div>
            <label style={{ display: 'block', fontSize: 11, color: '#ffd54f', fontWeight: 700, marginBottom: 4 }}>
              NEGATIVE MARKING DEDUCTED
            </label>
            <input
              type="number"
              step="0.25"
              placeholder="0"
              value={negMarks}
              onChange={(e) => setNegMarks(e.target.value)}
              style={{ width: '100%' }}
            />
          </div>

          {/* Calculated Total Bar */}
          <div
            style={{
              padding: '12px 16px',
              borderRadius: 'var(--radius-md)',
              background: 'rgba(208, 188, 255, 0.1)',
              border: '0.5px solid rgba(208, 188, 255, 0.25)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between'
            }}
          >
            <div>
              <div style={{ fontSize: 11, color: '#d0bcff', fontWeight: 700 }}>CALCULATED SCORE</div>
              <div style={{ fontFamily: 'var(--font-mono)', fontSize: 18, fontWeight: 800, color: '#fff' }}>
                {totalObtained} / {totalMax}
              </div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.6)' }}>PERCENTAGE</div>
              <div style={{ fontFamily: 'var(--font-mono)', fontSize: 18, fontWeight: 800, color: percent >= 75 ? '#81c784' : '#d0bcff' }}>
                {percent}%
              </div>
            </div>
          </div>

          {/* Buttons */}
          <div style={{ display: 'flex', gap: 10, marginTop: 4 }}>
            <button
              type="button"
              onClick={onDismiss}
              style={{
                flex: 1,
                padding: '10px 0',
                borderRadius: 'var(--radius-md)',
                border: '0.5px solid rgba(255, 255, 255, 0.15)',
                background: 'rgba(255, 255, 255, 0.05)',
                color: '#fff',
                fontSize: 13,
                fontWeight: 600,
                cursor: 'pointer'
              }}
            >
              Cancel
            </button>

            <button
              type="submit"
              style={{
                flex: 1,
                padding: '10px 0',
                borderRadius: 'var(--radius-md)',
                border: 'none',
                background: '#d0bcff',
                color: '#1d1b20',
                fontSize: 13,
                fontWeight: 700,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 6
              }}
            >
              <Check size={16} />
              <span>Save Marks</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
