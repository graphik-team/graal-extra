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

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.Substitution;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.util.stream.CloseableIterator;
import fr.lirmm.graphik.util.stream.IteratorException;

class BuiltInSubstitutionIterator<Q extends ConjunctiveQuery, F extends AtomSet> implements CloseableIterator<Substitution> {

	/**
	 * 
	 */
	private final ComplexHomomorphism<Q, F> complexHomomorphism;
	public BuiltInSubstitutionIterator(ComplexHomomorphism<Q, F> complexHomomorphism, CloseableIterator<Substitution> reader) {
		this.complexHomomorphism = complexHomomorphism;
		this.rawReader = reader;
	}

	@Override
	public boolean hasNext() throws IteratorException {
		if(this.next == null)
			this.next = this.computeNext();
		return this.next != null;
	}

	@Override
	public Substitution next() throws IteratorException {
		hasNext();
		Substitution res = this.next;
		this.next = null;
		return res;
	}

	protected Substitution computeNext() throws IteratorException {
		if (this.rawReader.hasNext()) {
			Substitution res = this.rawReader.next();
			if (check(res)) {
				return res;
			}
			else {
				return computeNext();
			}
		}
		else {
			return null;
		}
	}

	protected boolean check(Substitution s) {
		for (Atom a : this.complexHomomorphism.builtInAtoms) {
			Atom a2 = s.createImageOf(a);
			if (!((BuiltInPredicate)a2.getPredicate()).evaluate(a2.getTerms().toArray(new Term[a2.getTerms().size()]))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void close() {
		this.rawReader.close();
	}

	private Substitution next;
	private CloseableIterator<Substitution> rawReader;

}