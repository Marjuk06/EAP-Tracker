import {
  Subject,
  UserProfile,
  ExamRecord,
  DayStudyLog,
  EAPBackupSchema,
  StudySession
} from '../types';
import { initialSubjects, initialRoutines, defaultProfile } from '../data/initialData';

const STORAGE_KEYS = {
  PROFILE: 'eap_profile',
  SYLLABUS: 'eap_syllabus_state',
  EXAMS: 'eap_exam_records',
  ROUTINE: 'eap_routine_state',
  STUDY_LOGS: 'eap_study_logs',
  SESSIONS: 'eap_study_sessions'
};

type Listener = () => void;

class StorageService {
  private listeners: Set<Listener> = new Set();

  subscribe(listener: Listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  private notify() {
    this.listeners.forEach((fn) => fn());
  }

  // --- Profile ---
  getProfile(): UserProfile {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.PROFILE);
      if (data) {
        return { ...defaultProfile, ...JSON.parse(data) };
      }
    } catch {
      // fallback
    }
    return defaultProfile;
  }

  saveProfile(profile: Partial<UserProfile>) {
    const current = this.getProfile();
    const updated = { ...current, ...profile };
    localStorage.setItem(STORAGE_KEYS.PROFILE, JSON.stringify(updated));
    this.notify();
  }

  // --- Syllabus State ---
  getSyllabusState(): Record<string, boolean> {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.SYLLABUS);
      if (data) return JSON.parse(data);
    } catch {
      // fallback
    }
    return {};
  }

  saveSyllabusSection(subjectName: string, paperName: string, chapterName: string, sectionName: string, isCompleted: boolean) {
    const state = this.getSyllabusState();
    const key = `${subjectName}_${paperName}_${chapterName}_${sectionName}`;
    state[key] = isCompleted;
    localStorage.setItem(STORAGE_KEYS.SYLLABUS, JSON.stringify(state));
    this.notify();
  }

  getSubjects(): Subject[] {
    const state = this.getSyllabusState();
    return initialSubjects.map((sub) => ({
      ...sub,
      papers: sub.papers.map((pap) => ({
        ...pap,
        chapters: pap.chapters.map((chap) => ({
          ...chap,
          sections: chap.sections.map((sec) => {
            const key = `${sub.name}_${pap.name}_${chap.name}_${sec.name}`;
            return {
              ...sec,
              isCompleted: !!state[key]
            };
          })
        }))
      }))
    }));
  }

  resetSyllabus() {
    localStorage.removeItem(STORAGE_KEYS.SYLLABUS);
    this.notify();
  }

  // --- Exam Records ---
  getExams(): ExamRecord[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.EXAMS);
      if (data) return JSON.parse(data);
    } catch {
      // fallback
    }
    return [];
  }

  saveExam(exam: Omit<ExamRecord, 'id'> & { id?: string }) {
    const exams = this.getExams();
    const id = exam.id || `exam-${Date.now()}`;
    const existingIndex = exams.findIndex((e) => e.id === id);
    const updatedExam: ExamRecord = { ...exam, id };

    if (existingIndex >= 0) {
      exams[existingIndex] = updatedExam;
    } else {
      exams.unshift(updatedExam);
    }

    localStorage.setItem(STORAGE_KEYS.EXAMS, JSON.stringify(exams));
    this.notify();
  }

  deleteExam(id: string) {
    const exams = this.getExams().filter((e) => e.id !== id);
    localStorage.setItem(STORAGE_KEYS.EXAMS, JSON.stringify(exams));
    this.notify();
  }

  resetExams() {
    localStorage.removeItem(STORAGE_KEYS.EXAMS);
    this.notify();
  }

  // --- Routine State ---
  getRoutineState(): Record<string, boolean> {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.ROUTINE);
      if (data) return JSON.parse(data);
    } catch {
      // fallback
    }
    return {};
  }

  toggleRoutineItem(itemId: string, isCompleted: boolean) {
    const state = this.getRoutineState();
    state[itemId] = isCompleted;
    localStorage.setItem(STORAGE_KEYS.ROUTINE, JSON.stringify(state));
    this.notify();
  }

  getRoutines() {
    const state = this.getRoutineState();
    return initialRoutines.map((item) => ({
      ...item,
      isCompleted: !!state[item.id]
    }));
  }

  // --- Study Logs & Sessions (Pomodoro & Study Tracker) ---
  getStudyLogs(): DayStudyLog[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.STUDY_LOGS);
      if (data) return JSON.parse(data);
    } catch {
      // fallback
    }
    return [];
  }

  getTodayStudyMinutes(): number {
    const today = new Date().toISOString().split('T')[0];
    const logs = this.getStudyLogs();
    const todayLog = logs.find((l) => l.date === today);
    return todayLog ? todayLog.minutes : 0;
  }

  logStudyMinutes(minutes: number, subject?: string, notes?: string) {
    if (minutes <= 0) return;
    const today = new Date().toISOString().split('T')[0];
    const logs = this.getStudyLogs();
    const existingIndex = logs.findIndex((l) => l.date === today);

    if (existingIndex >= 0) {
      logs[existingIndex].minutes += minutes;
    } else {
      logs.push({ date: today, minutes });
    }

    localStorage.setItem(STORAGE_KEYS.STUDY_LOGS, JSON.stringify(logs));

    // Save detailed session
    const sessions = this.getStudySessions();
    const newSession: StudySession = {
      id: `sess-${Date.now()}`,
      timestamp: Date.now(),
      durationMinutes: minutes,
      subject,
      notes
    };
    sessions.unshift(newSession);
    localStorage.setItem(STORAGE_KEYS.SESSIONS, JSON.stringify(sessions.slice(0, 200)));

    this.notify();
  }

  getStudySessions(): StudySession[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.SESSIONS);
      if (data) return JSON.parse(data);
    } catch {
      // fallback
    }
    return [];
  }

  // Calculate Streak
  getStudyStreak(): number {
    const logs = this.getStudyLogs().filter((l) => l.minutes > 0);
    if (logs.length === 0) return 0;

    const logDates = new Set(logs.map((l) => l.date));
    let streak = 0;
    const checkDate = new Date();

    // Check if today has study minutes
    const todayStr = checkDate.toISOString().split('T')[0];
    if (!logDates.has(todayStr)) {
      // If not today, check if yesterday was active
      checkDate.setDate(checkDate.getDate() - 1);
      const yesterdayStr = checkDate.toISOString().split('T')[0];
      if (!logDates.has(yesterdayStr)) {
        return 0;
      }
    }

    while (true) {
      const dateStr = checkDate.toISOString().split('T')[0];
      if (logDates.has(dateStr)) {
        streak++;
        checkDate.setDate(checkDate.getDate() - 1);
      } else {
        break;
      }
    }

    return streak;
  }

  // --- Backup & Restore (100% Android App Compatible) ---
  exportBackupJson(): string {
    const payload: EAPBackupSchema = {
      version: 2,
      exportedAt: Date.now(),
      profile: this.getProfile(),
      syllabusState: this.getSyllabusState(),
      exams: this.getExams(),
      routineState: this.getRoutineState(),
      studyLogs: this.getStudyLogs()
    };
    return JSON.stringify(payload, null, 2);
  }

  downloadBackupFile() {
    const jsonStr = this.exportBackupJson();
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const dateStr = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
    const a = document.createElement('a');
    a.href = url;
    a.download = `EAPTracker_Backup_${dateStr}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  importBackupJson(jsonStr: string): boolean {
    try {
      const data = JSON.parse(jsonStr);
      if (!data || typeof data !== 'object') return false;

      if (data.profile) {
        localStorage.setItem(STORAGE_KEYS.PROFILE, JSON.stringify({ ...defaultProfile, ...data.profile }));
      }
      if (data.syllabusState && typeof data.syllabusState === 'object') {
        localStorage.setItem(STORAGE_KEYS.SYLLABUS, JSON.stringify(data.syllabusState));
      }
      if (Array.isArray(data.exams)) {
        localStorage.setItem(STORAGE_KEYS.EXAMS, JSON.stringify(data.exams));
      }
      if (data.routineState && typeof data.routineState === 'object') {
        localStorage.setItem(STORAGE_KEYS.ROUTINE, JSON.stringify(data.routineState));
      }
      if (Array.isArray(data.studyLogs)) {
        localStorage.setItem(STORAGE_KEYS.STUDY_LOGS, JSON.stringify(data.studyLogs));
      }

      this.notify();
      return true;
    } catch {
      return false;
    }
  }

  clearAllData() {
    localStorage.clear();
    this.notify();
  }
}

export const storage = new StorageService();
