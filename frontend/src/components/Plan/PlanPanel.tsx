import React, { useState, useEffect } from 'react';
import { websocketService } from '../../services/websocket';
import { cn, getBadgeClasses } from '../../theme';

interface TodoStructure {
  title: string;
  tasks: TodoTask[];
  sections: Record<string, string>;
  lastUpdated: string;
}

interface TodoTask {
  taskNumber: number;
  description: string;
  completed: boolean;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
  notes?: string;
}

interface PlanPanelProps {
  sessionId: string;
}

export const PlanPanel: React.FC<PlanPanelProps> = ({ sessionId }) => {
  const [plan, setPlan] = useState<TodoStructure | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadPlan();
    startWatching();
    
    // WebSocket for live updates
    const subscription = websocketService.subscribe(
      `/topic/plan/${sessionId}`,
      (message: any) => {
        const updatedPlan = message as TodoStructure;
        setPlan(updatedPlan);
      }
    );
    
    return () => subscription?.unsubscribe();
  }, [sessionId]);

  const loadPlan = async () => {
    try {
      const response = await fetch(`/api/plan/${sessionId}`);
      if (response.ok) {
        const data = await response.json();
        setPlan(data);
      }
    } catch (error) {
      console.error('Failed to load plan:', error);
    } finally {
      setLoading(false);
    }
  };

  const startWatching = async () => {
    try {
      await fetch(`/api/plan/${sessionId}/watch`, { method: 'POST' });
    } catch (error) {
      console.error('Failed to start watching:', error);
    }
  };

  const getStatusIcon = (status: TodoTask['status']) => {
    switch (status) {
      case 'COMPLETED':
        return '✅';
      case 'IN_PROGRESS':
        return '🔄';
      default:
        return '⏳';
    }
  };

  const getStatusColor = (status: TodoTask['status']) => {
    switch (status) {
      case 'COMPLETED':
        return 'border-green-500/50 bg-green-900/20 hover:bg-green-900/30';
      case 'IN_PROGRESS':
        return 'border-blue-500/50 bg-blue-900/20 hover:bg-blue-900/30';
      default:
        return 'border-gray-700/50 bg-gray-800/40 hover:bg-gray-800/60';
    }
  };

  if (loading) {
    return (
      <div className="h-full flex items-center justify-center bg-gradient-to-b from-gray-900 to-gray-900/95">
        <div className="text-center space-y-3">
          <div className="text-4xl animate-pulse">📋</div>
          <div className="text-gray-400">Loading plan...</div>
        </div>
      </div>
    );
  }

  if (!plan) {
    return (
      <div className="h-full flex items-center justify-center bg-gradient-to-b from-gray-900 to-gray-900/95">
        <div className="text-center space-y-4">
          <div className="text-6xl opacity-30">📋</div>
          <div>
            <p className="text-lg font-medium text-gray-400">No Plan Yet</p>
            <p className="text-sm text-gray-600 mt-2">
              Agent will create todo.md when planning
            </p>
          </div>
        </div>
      </div>
    );
  }

  const completedCount = plan.tasks.filter(t => t.completed).length;
  const progress = plan.tasks.length > 0 ? (completedCount / plan.tasks.length) * 100 : 0;

  return (
    <div className="h-full flex flex-col bg-gradient-to-b from-gray-900 to-gray-900/95">
      {/* Header */}
      <div className="border-b border-gray-700/50 px-4 py-4 bg-gray-800/40 backdrop-blur-sm">
        <div className="flex items-center gap-3 mb-3">
          <span className="text-2xl">📋</span>
          <h2 className="text-lg font-semibold text-gray-200">{plan.title}</h2>
        </div>
        
        <div className="flex items-center justify-between text-xs mb-3">
          <span className="text-gray-400">
            {completedCount} of {plan.tasks.length} tasks completed
          </span>
          <span className={cn(getBadgeClasses('info'), 'text-xs')}>
            {Math.round(progress)}%
          </span>
        </div>

        {/* Progress Bar */}
        <div className="relative bg-gray-700/50 rounded-full h-2 overflow-hidden">
          <div
            className="absolute inset-y-0 left-0 bg-gradient-to-r from-blue-500 to-blue-600 rounded-full transition-all duration-500 ease-out"
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

      {/* Tasks */}
      <div className="flex-1 overflow-y-auto p-4 custom-scrollbar">
        {plan.tasks.length === 0 ? (
          <div className="text-center text-gray-400 py-12">
            <div className="text-4xl mb-3 opacity-50">📝</div>
            <p>No tasks in plan yet</p>
          </div>
        ) : (
          <div className="space-y-3">
            {plan.tasks.map((task) => (
              <div
                key={task.taskNumber}
                className={cn(
                  'p-4 rounded-xl border transition-all duration-200',
                  getStatusColor(task.status)
                )}
              >
                <div className="flex items-start gap-3">
                  <span className="text-2xl flex-shrink-0 mt-0.5">
                    {getStatusIcon(task.status)}
                  </span>
                  <div className="flex-1 min-w-0">
                    <div className={cn(
                      'font-medium leading-relaxed',
                      task.completed ? 'line-through text-gray-500' : 'text-gray-200'
                    )}>
                      <span className="inline-flex items-center justify-center w-6 h-6 rounded-full bg-gray-700/50 text-gray-400 text-xs mr-2">
                        {task.taskNumber}
                      </span>
                      {task.description}
                    </div>
                    {task.notes && (
                      <div className="text-sm text-gray-400 mt-2 pl-8 leading-relaxed">
                        💡 {task.notes}
                      </div>
                    )}
                    {task.status === 'IN_PROGRESS' && (
                      <div className="mt-3 flex items-center gap-2 text-sm text-blue-400 pl-8">
                        <div className="w-4 h-4 border-2 border-blue-400 border-t-transparent rounded-full animate-spin"></div>
                        <span>In progress...</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Additional Sections */}
      {Object.entries(plan.sections || {}).map(([sectionName, content]) => (
        sectionName !== 'tasks' && content && (
          <div key={sectionName} className="border-t border-gray-700/50 px-4 py-3 max-h-32 overflow-y-auto bg-gray-800/30 custom-scrollbar">
            <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2 flex items-center gap-2">
              <span className="w-1 h-4 bg-blue-500 rounded"></span>
              {sectionName}
            </h3>
            <div className="text-sm text-gray-300 whitespace-pre-wrap leading-relaxed">
              {content}
            </div>
          </div>
        )
      ))}

      {/* Footer */}
      <div className="border-t border-gray-700/50 px-4 py-2.5 bg-gray-800/40 backdrop-blur-sm">
        <div className="text-xs text-gray-500 flex items-center gap-2">
          <span className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"></span>
          Last updated: {new Date(plan.lastUpdated).toLocaleTimeString([], {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
          })}
        </div>
      </div>
    </div>
  );
};
