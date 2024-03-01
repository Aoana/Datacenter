import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyHost extends Host {
    /* queue for tasks, compare after priority in descending order and then after who came first*/
    private final Queue<Task> taskQueue = new PriorityQueue<>(Comparator.comparingInt(Task::getPriority).reversed().thenComparing(Task::getId));
    private Task currentTask = null;
    private AtomicBoolean finish = new AtomicBoolean(false);

    @Override
    public void run() {
        while (!finish.get()) {
                preemptIfNecessary();

            boolean finished = executeTask();

            /* if it finished , wait for the other task */
            if (finished && taskQueue.isEmpty()) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

        }
    }

    /* function to get the task with higher priority */
    private Task getHigherPriority() {
        Task nextTask = peekTask();

        if (currentTask == null || (nextTask != null && nextTask.getPriority() > currentTask.getPriority())) {
            return nextTask;
        }

        return currentTask;
    }

    /* return true if the preemptible property is accomplished */
    private boolean preemptible(Task t1, Task t2) {
        /* if it has the property to be preemptible and the next task has bigger priority*/
        return t1 == null || (t1.isPreemptible() && t2.getPriority() > t1.getPriority());
    }

    /* make the effective exchange if a task is preemptible and another has to take it s place */
    public void preemptIfNecessary() {
        Task nextTask = getHigherPriority();

        if ((currentTask != null && nextTask != null && nextTask.compareTo(currentTask) == 0)
                || (currentTask == null && nextTask == null)) {
            return;
        }

        if (nextTask != null && preemptible(currentTask, nextTask)) {
            pollTask();

            if (currentTask != null) {
                addTask(currentTask);
            }
            currentTask = nextTask;
        }
    }

    /* execute task for the exact amount of seconds */
    private boolean executeTask() {
        if (currentTask == null) {
            return false;
        }

        long remainingTime = currentTask.getLeft();

        if (remainingTime > 0) {
            process();

            remainingTime -= TimeUnit.SECONDS.toMillis(1);
            currentTask.setLeft(remainingTime);
        }

        if (remainingTime <= 0) {
            currentTask.finish();
            currentTask = null;
            return true;
        }
        return false;
    }

    @Override
    public void addTask(Task task) {
            taskQueue.add(task);
    }

    @Override
    public int getQueueSize() {
            return taskQueue.size();
    }

    private Task peekTask() {
            return taskQueue.peek();
    }

    private void pollTask() {
            taskQueue.poll();
    }

    /* make the sum of work left in the queue */
    @Override
    public long getWorkLeft() {
            if (!taskQueue.isEmpty()) {
                return taskQueue.stream().mapToLong(Task::getLeft).sum();
            }

        return 0;
    }

    /* see if a task is running */
    protected boolean existsRunningTask() {
        synchronized (this) {
            return currentTask != null;
        }
    }

    /* if task is running get the remaining work time */
    protected long runningTaskLeft() {
        synchronized (this) {
            if (currentTask != null)
                return currentTask.getLeft();
        }
        return 0;
    }

    /* process if it still has time */
    void process() {
        double start = Timer.getTimeDouble();
        double end = Timer.getTimeDouble();

        while (end - start <= TimeUnit.SECONDS.toSeconds(1))
        {
            end = Timer.getTimeDouble();
        }
    }

    /* put on finish if the task is done */
    @Override
    public void shutdown() {
        while (getWorkLeft() != 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        finish.set(true);

        /* notify when another thread comes */
        synchronized (this) {
            this.notify();
        }
    }
}
