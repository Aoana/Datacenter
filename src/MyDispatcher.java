import java.util.List;

public class MyDispatcher extends Dispatcher {
    private int lastAssignedHostIndex = -1;

    public MyDispatcher(SchedulingAlgorithm algorithm, List<Host> hosts) {
        super(algorithm, hosts);
    }

    /* made a function for each planning policies*/
    @Override
    public void addTask(Task task) {
        switch (algorithm) {
            case ROUND_ROBIN -> roundRobin(task);
            case SHORTEST_QUEUE -> shortestQueue(task);
            case SIZE_INTERVAL_TASK_ASSIGNMENT -> sizeIntervalTaskAssignment(task);
            case LEAST_WORK_LEFT -> leastWorkLeft(task);
            default -> throw new IllegalArgumentException("Invalid scheduling algorithm");
        }
    }

    private void roundRobin(Task task) {
        synchronized (this) {
            lastAssignedHostIndex = (lastAssignedHostIndex + 1) % hosts.size();
            Host nextHost = hosts.get(lastAssignedHostIndex);

            synchronized (nextHost) {
                nextHost.addTask(task);
                nextHost.notify();
            }
        }
    }

    private void shortestQueue(Task task) {
        synchronized (this) {
            Host nextHost = null;
            int minQueueSize = Integer.MAX_VALUE;

            for (Host host : hosts) {
                synchronized (host) {
                    MyHost myHost = (MyHost) host;
                    int totalSize = host.getQueueSize();
                    /* get the queue size of host and add 1 if it has a running task */
                    if (myHost.existsRunningTask())
                        totalSize++;

                    if (totalSize < minQueueSize) {
                        nextHost = host;
                        minQueueSize = totalSize;
                        /* if equal send the task to the host with smaller ID*/
                    } else if (totalSize == minQueueSize) {
                        int selectedHostId = (nextHost != null) ? (int) nextHost.getId() : Integer.MAX_VALUE;
                        int currentHostId = (int) host.getId();

                        if (currentHostId > selectedHostId) {
                            nextHost = host;
                        }
                    }
                }
            }

            synchronized (nextHost) {
                nextHost.addTask(task);
                nextHost.notify();
            }
        }
    }

    private void sizeIntervalTaskAssignment(Task task) {
        synchronized (this) {
            Host nextHost = null;
            TaskType type = task.getType();
            switch (type) {
                /* assign task to the corresponding node*/
                case SHORT -> nextHost = hosts.get(0);
                case MEDIUM -> nextHost = hosts.get(1);
                case LONG -> nextHost = hosts.get(2);
                default -> throw new IllegalArgumentException("Invalid host");
            }

            synchronized (nextHost) {
                nextHost.addTask(task);
                nextHost.notify();
            }
        }
    }

    private void leastWorkLeft(Task task) {
        synchronized (this) {
            Host nextHost = null;
            long minWorkLeft = Long.MAX_VALUE;

            for (Host host : hosts) {
                MyHost myHost = (MyHost) host;
                /* add the work left for tasks in queue and task that is currently running */
                long workLeft = host.getWorkLeft() + myHost.runningTaskLeft();

                if (workLeft < minWorkLeft) {
                    minWorkLeft = workLeft;
                    nextHost = host;
                    /* if equal for both hosts, compare after ID*/
                } else if (workLeft == minWorkLeft) {
                    int selectedHostId = (nextHost != null) ? (int) nextHost.getId() : Integer.MAX_VALUE;
                    int currentHostId = (int) host.getId();

                    if (currentHostId > selectedHostId) {
                        nextHost = host;
                    }
                }
            }

            synchronized (nextHost) {
                nextHost.addTask(task);
                nextHost.notify();
            }
        }
    }
}
