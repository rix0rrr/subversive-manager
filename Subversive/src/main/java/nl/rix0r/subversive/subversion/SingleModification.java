
package nl.rix0r.subversive.subversion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A single modification wraps a number of other modifications, so they
 * are undone as one.
 *
 * @author rix0rrr
 */
public class SingleModification implements Modification {
    private List<Modification> modifications = new ArrayList<Modification>();
    private String repository = "";

    public SingleModification() {
    }

    public SingleModification(Collection<Modification> modifications) {
        for (Modification mod: modifications)
            add(mod);
    }

    public void add(Modification modification) {
        if (repository.equals("")) repository = modification.repository();
        if (!repository.equals(modification.repository()))
            throw new RuntimeException("SingleModification: all modifications must be on the same repository! (Previous: " + repository + ", new: " + modification.repository());
        modifications.add(modification);
    }

    public Collection<Modification> modifications() {
        return Collections.unmodifiableList(modifications);
    }

    public void apply(Configuration configuration) {
        for (Modification mod: modifications)
            mod.apply(configuration);
    }

    public String repository() {
        return repository;
    }

}
