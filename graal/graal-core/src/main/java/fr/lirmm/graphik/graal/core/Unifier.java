/**
 * 
 */
package fr.lirmm.graphik.graal.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import fr.lirmm.graphik.graal.core.atomset.AtomSet;
import fr.lirmm.graphik.graal.core.atomset.InMemoryAtomSet;
import fr.lirmm.graphik.graal.core.factory.SubstitutionFactory;
import fr.lirmm.graphik.util.LinkedSet;

/**
 * @author Clément Sipieter (INRIA) {@literal <clement@6pi.fr>}
 * 
 */
public class Unifier {

	private static Unifier instance;

	protected Unifier() {
	}

	public static synchronized Unifier getInstance() {
		if (instance == null)
			instance = new Unifier();

		return instance;
	}

	// /////////////////////////////////////////////////////////////////////////
	// METHODS
	// /////////////////////////////////////////////////////////////////////////

	/**
	 * FIXME possible conflict on variable name: the main problem is provide by
	 * the computation of pieces unifiers between the same rule. The question is
	 * how store them. {X -> X} is ambiguous.
	 * 
	 * @param rule
	 * @param atomset
	 * @return
	 */
	public Set<Substitution> computePieceUnifier(Rule rule, AtomSet atomset) {
		// FIXME

		Set<Substitution> unifiers = new LinkedSet<Substitution>();
		Queue<Atom> atomQueue = new LinkedList<Atom>();
		for (Atom a : atomset) {
			atomQueue.add(a);
		}

		for (Atom a : atomset) {
			Queue<Atom> tmp = new LinkedList<Atom>(atomQueue);
			unifiers.addAll(extendUnifier(rule, tmp, a,
					new TreeMapSubstitution()));
		}
		return unifiers;
	}

	public boolean existPieceUnifier(Rule rule, InMemoryAtomSet atomset) {
		FreeVarSubstitution substitution = new FreeVarSubstitution();
		InMemoryAtomSet atomsetSubstitut = substitution.getSubstitut(atomset);

		Queue<Atom> atomQueue = new LinkedList<Atom>();
		for (Atom a : atomsetSubstitut) {
			atomQueue.add(a);
		}

		for (Atom a : atomsetSubstitut) {
			Queue<Atom> tmp = new LinkedList<Atom>(atomQueue);
			if (existExtendedUnifier(rule, tmp, a, new TreeMapSubstitution())) {
				return true;
			}
		}
		return false;
	}

	// /////////////////////////////////////////////////////////////////////////
	// PRIVATE FUNCTIONS
	// /////////////////////////////////////////////////////////////////////////

	private static Collection<Substitution> extendUnifier(Rule rule,
			Queue<Atom> atomset, Atom pieceElement, Substitution unifier) {
		atomset.remove(pieceElement);
		Collection<Substitution> unifierCollection = new LinkedList<Substitution>();
		Set<Term> frontierVars = rule.getFrontier();
		Set<Term> existentialVars = rule.getExistentials();

		for (Atom atom : rule.getHead()) {
			Substitution u = unifier(unifier, pieceElement, atom, frontierVars,
					existentialVars);
			if (u != null) {
				Iterator<Atom> it = atomset.iterator();
				Atom newPieceElement = null;
				while (it.hasNext() && newPieceElement == null) {
					Atom a = it.next();

					for (Term t : a) {
						if (existentialVars.contains(u.getSubstitute(t))) {
							newPieceElement = a;
							break;
						}
					}

				}

				if (newPieceElement == null) {
					unifierCollection.add(u);
				} else {
					unifierCollection.addAll(extendUnifier(rule, atomset,
							newPieceElement, u));
				}
			}
		}
		return unifierCollection;
	}

	private static Substitution unifier(Substitution baseUnifier, Atom a1,
			Atom atomFromHead, Set<Term> frontierVars, Set<Term> existentialVars) {
		if (a1.getPredicate().equals(atomFromHead.getPredicate())) {
			boolean error = false;
			Substitution u = SubstitutionFactory.getInstance()
					.createSubstitution();
			u.put(baseUnifier);
			for (int i = 0; i < a1.getPredicate().getArity(); ++i) {
				Term t1 = a1.getTerm(i);
				Term t2 = atomFromHead.getTerm(i);
				if (!t1.equals(t2)) {
					if (Term.Type.VARIABLE.equals(t1.getType())) {
						if (!compose(u, frontierVars, existentialVars, t1, t2))
							error = true;
					} else if (Term.Type.VARIABLE.equals(t2.getType())
							&& !existentialVars.contains(t2)) {
						if (!compose(u, frontierVars, existentialVars, t2, t1))
							error = true;
					} else {
						error = true;
					}
				}
			}

			if (!error)
				return u;
		}

		return null;
	}

	private static boolean compose(Substitution u, Set<Term> frontierVars,
			Set<Term> existentials, Term term, Term substitut) {
		Term termSubstitut = u.getSubstitute(term);
		Term substitutSubstitut = u.getSubstitute(substitut);

		if (Term.Type.CONSTANT.equals(termSubstitut.getType())
				|| existentials.contains(termSubstitut)) {
			Term tmp = termSubstitut;
			termSubstitut = substitutSubstitut;
			substitutSubstitut = tmp;
		}

		for (Term t : u.getTerms()) {
			if (termSubstitut.equals(u.getSubstitute(t))) {
				if (!put(u, frontierVars, existentials, t, substitutSubstitut)) {
					return false;
				}
			}
		}

		if (!put(u, frontierVars, existentials, termSubstitut, substitutSubstitut)) {
			return false;
		}
		return true;
	}

	private static boolean put(Substitution u, Set<Term> frontierVars,
			Set<Term> existentials, Term term, Term substitut) {
		if (!term.equals(substitut)) {
			// two (constant | existentials vars)
			if (Term.Type.CONSTANT.equals(term.getType())
					|| existentials.contains(term)) {
				return false;
				// fr -> existential vars
			} else if (frontierVars.contains(term)
					&& existentials.contains(substitut)) {
				return false;
			}
		}
		return u.put(term, substitut);
	}

	private static boolean existExtendedUnifier(Rule rule, Queue<Atom> atomset,
			Atom pieceElement, Substitution unifier) {
		atomset.remove(pieceElement);
		Set<Term> frontierVars = rule.getFrontier();
		Set<Term> existentialVars = rule.getExistentials();

		for (Atom atom : rule.getHead()) {
			Substitution u = unifier(unifier, pieceElement, atom, frontierVars,
					existentialVars);
			if (u != null) {
				Iterator<Atom> it = atomset.iterator();
				Atom newPieceElement = null;
				while (it.hasNext() && newPieceElement == null) {
					Atom a = it.next();

					for (Term t : a) {
						if (existentialVars.contains(u.getSubstitute(t))) {
							newPieceElement = a;
							break;
						}
					}

				}

				if (newPieceElement == null) {
					return true;
				} else {
					if (existExtendedUnifier(rule, atomset, newPieceElement, u)) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
