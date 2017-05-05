/*
 * Copyright (C) Inria Sophia Antipolis - Méditerranée / LIRMM
 * (Université de Montpellier & CNRS) (2014 - 2017)
 *
 * Contributors :
 *
 * Clément SIPIETER <clement.sipieter@inria.fr>
 * Mélanie KÖNIG
 * Swan ROCHER
 * Jean-François BAGET
 * Michel LECLÈRE
 * Marie-Laure MUGNIER <mugnier@lirmm.fr>
 *
 *
 * This file is part of Graal <https://graphik-team.github.io/graal/>.
 *
 * This software is governed by the CeCILL  license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */
 package fr.lirmm.graphik.graal.builtin;

import java.util.LinkedList;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.InMemoryAtomSet;
import fr.lirmm.graphik.graal.api.core.Substitution;
import fr.lirmm.graphik.graal.api.homomorphism.Homomorphism;
import fr.lirmm.graphik.graal.api.homomorphism.HomomorphismException;
import fr.lirmm.graphik.graal.core.atomset.LinkedListAtomSet;
import fr.lirmm.graphik.graal.core.factory.DefaultConjunctiveQueryFactory;
import fr.lirmm.graphik.graal.homomorphism.AbstractHomomorphism;
import fr.lirmm.graphik.util.stream.CloseableIterator;
import fr.lirmm.graphik.util.stream.CloseableIteratorWithoutException;

/**
 * This Homomorphism implementation manage builtInPredicate by a post filtering
 * @author Clément Sipieter (INRIA) {@literal <clement@6pi.fr>}
 *
 * @param <Q>
 * @param <F>
 */
public class ComplexHomomorphism<Q extends ConjunctiveQuery, F extends AtomSet> extends AbstractHomomorphism<Q, F>
                                implements Homomorphism<Q, F> {

	private Homomorphism<ConjunctiveQuery,F> rawSolver;
	LinkedList<Atom> builtInAtoms;

	public ComplexHomomorphism(Homomorphism<ConjunctiveQuery,F> rawSolver) {
		this.rawSolver = rawSolver;
	}

	@Override
	public <U1 extends Q, U2 extends F> CloseableIterator<Substitution> execute(U1 q, U2 f)
			throws HomomorphismException {
    	InMemoryAtomSet rawAtoms = new LinkedListAtomSet();
		this.builtInAtoms = new LinkedList<Atom>();
		CloseableIteratorWithoutException<Atom> it = q.iterator();
		while (it.hasNext()) {
			Atom a = it.next();
			if (a.getPredicate() instanceof BuiltInPredicate) {
				this.builtInAtoms.add(a);
			}
			else {
				rawAtoms.add(a);
			}
		}
		ConjunctiveQuery rawQuery = DefaultConjunctiveQueryFactory.instance().create(rawAtoms);
		rawQuery.setAnswerVariables(q.getAnswerVariables());
		return new BuiltInSubstitutionIterator<Q, F>(this, this.rawSolver.execute(rawQuery,f));
	}

};

