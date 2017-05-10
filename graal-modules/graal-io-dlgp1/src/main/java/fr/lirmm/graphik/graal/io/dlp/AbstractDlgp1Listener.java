/*
 * Copyright (C) Inria Sophia Antipolis - Méditerranée / LIRMM
 * (Université de Montpellier & CNRS) (2014 - 2015)
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
 /**
 * 
 */
package fr.lirmm.graphik.graal.io.dlp;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.lirmm.graphik.graal.api.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Rule;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.core.DefaultAtom;
import fr.lirmm.graphik.graal.core.DefaultNegativeConstraint;
import fr.lirmm.graphik.graal.core.atomset.LinkedListAtomSet;
import fr.lirmm.graphik.graal.core.factory.DefaultConjunctiveQueryFactory;
import fr.lirmm.graphik.graal.core.factory.DefaultRuleFactory;
import parser.ParserListener;

/**
 * @author Clément Sipieter (INRIA) <clement@6pi.fr>
 * 
 */
abstract class AbstractDlgp1Listener implements ParserListener {
    
	private static final Logger LOGGER = LoggerFactory
			.getLogger(AbstractDlgp1Listener.class);
	
	private List<Term> answerVars;
	private LinkedListAtomSet atomSet = null;
	private LinkedListAtomSet atomSet2 = null;
	private DefaultAtom atom;
	private String label;

	private OBJECT_TYPE objectType;

	protected abstract void createAtom(DefaultAtom atom);

	protected abstract void createQuery(ConjunctiveQuery query);
	
	protected abstract void createRule(Rule basicRule);
	
	protected abstract void createNegConstraint(DefaultNegativeConstraint negativeConstraint);

	@Override
	public void startsObject(OBJECT_TYPE objectType, String name) {
		this.label = name == null? "" : name;
		
		atomSet = atomSet2 = null;
		this.objectType = objectType;
		
		switch (objectType) {
		case QUERY:
			this.answerVars = new LinkedList<Term>();
			this.atomSet = new LinkedListAtomSet();
			break;
		case RULE:
		case NEG_CONSTRAINT:
			this.atomSet = new LinkedListAtomSet();
			break;
		case FACT:
			break;
		default:
			if(LOGGER.isWarnEnabled()) {
				LOGGER.warn("Unrecognized object type: " + objectType);
			}
			break;
		}
		
	}
	
	

	@Override
	public void createsAtom(String predicate, Object[] terms) {

		List<Term> list = new LinkedList<Term>();
		for (Object t : terms)
			list.add((Term) t);
		
		String predicateWithoutQuotes = removeQuotes(predicate);

		atom = new DefaultAtom(new Predicate(predicateWithoutQuotes, terms.length), list);

		switch (objectType) {
		case FACT:
			this.createAtom(atom);
			break;
		case QUERY:
		case RULE:
		case NEG_CONSTRAINT:
			this.atomSet.add(atom);
			break;
		default:
			break;
		}
	}

	/**
	 * @param predicate
	 */
	private String removeQuotes(String predicate) {
		if(predicate.startsWith("\"") && predicate.endsWith("\"")) {
			return predicate.substring(1, predicate.length() - 1);
		} else {
			return predicate;
		}
	}

	@Override
	public void createsEquality(Object term1, Object term2) {
		atom = new DefaultAtom(Predicate.EQUALITY, (Term) term1, (Term) term2);

		switch (objectType) {
		case FACT:
			this.createAtom(atom);
			break;
		case QUERY:
		case RULE:
		case NEG_CONSTRAINT:
			this.atomSet.add(atom);
			break;
		default:
			break;
		}
	}

	@Override
	public void answerVariableList(Object[] terms) {
		for (Object t : terms)
			this.answerVars.add((Term) t);
	}

	@Override
	public void endsConjunction(OBJECT_TYPE objectType) {
		switch (objectType) {
		case QUERY:
			this.createQuery(DefaultConjunctiveQueryFactory.instance().create(this.label, this.atomSet, this.answerVars));
			break;
		case NEG_CONSTRAINT:
			this.createNegConstraint(new DefaultNegativeConstraint(this.label, this.atomSet));
			break;
		case RULE:
			if(this.atomSet2 == null) {
    			this.atomSet2 = this.atomSet;
    			this.atomSet = new LinkedListAtomSet();
			} else {
				this.createRule(DefaultRuleFactory.instance().create(this.label, this.atomSet, this.atomSet2));
			}
			break;
		default:
			break;
		}
	}

}