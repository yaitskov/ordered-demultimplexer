import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 */
public interface FailedActorJudge {
    /**
     * @return true no actors left
     */
    boolean punish(AtomicReferenceArray<Actor> actors, int i, RuntimeException e);
}
