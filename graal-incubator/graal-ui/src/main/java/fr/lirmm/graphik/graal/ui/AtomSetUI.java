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
 * This software is governed by the CeCILL  license under French law andattribute
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
package fr.lirmm.graphik.graal.ui;

import java.util.Set;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.spriteManager.SpriteManager;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSet;
import fr.lirmm.graphik.graal.api.core.Constant;
import fr.lirmm.graphik.graal.api.core.ConstantGenerator;
import fr.lirmm.graphik.graal.api.core.InMemoryAtomSet;
import fr.lirmm.graphik.graal.api.core.Literal;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.api.core.Term.Type;
import fr.lirmm.graphik.graal.api.core.Variable;
import fr.lirmm.graphik.util.stream.CloseableIterator;
import fr.lirmm.graphik.util.stream.CloseableIteratorWithoutException;
import fr.lirmm.graphik.util.stream.IteratorException;

/**
 * @author Clément Sipieter (INRIA) {@literal <clement@6pi.fr>}
 *
 */
public class AtomSetUI implements InMemoryAtomSet {

	private InMemoryAtomSet atomset;
	Graph graph = new MultiGraph("Test");

	SpriteManager sman = new SpriteManager(graph);
	private int node = 0;
	private int edge = 0;

	public AtomSetUI(InMemoryAtomSet atomset) throws IteratorException {
		this.atomset = atomset;
		graph.setStrict(false);
		graph.setAutoCreate(true);
		graph.addAttribute("ui.antialias");
		graph.addAttribute("ui.stylesheet",
						"node { " 
								+ "  size: 20px, 20px;                      " 
								+ " 	fill-color: #EF3;    "
						+ "stroke-mode: plain;               " + " 	stroke-color: #222; stroke-width: 2px;  " + " 	 "
						+ ""
						+ ""
						+ "   " + "                   " + " }                                       "
						+ "                                         " 
						+ " edge {      " + "                            "
						+ " 	shape: line;" + "  size: 2px;                  "
						+ " 	fill-color: #222;                   " + " 	arrow-size: 7px, 7px;               "
						+ " }    "
						+ "                                   "
						+ "node.concept {"
						+ " "
						+ "size: 40px, 10px;"
						+ "fill-color: #0FF;"
						+ "shape: rounded-box;stroke-mode: plain;stroke-color: #222;stroke-width: 2px; "
						+ "}"
						+ ""
						+ "edge.new { fill-color: #F00; size: 3px; }"
						+ ""

		);

		graph.display();
		CloseableIterator<Atom> it = this.atomset.iterator();
		while (it.hasNext()) {
			Atom next = it.next();
			graph.addEdge(next.getPredicate().getIdentifier().toString(), next.getTerm(0).getIdentifier().toString(),
					next.getTerm(1).getIdentifier().toString());
		}
	}

	/**
	 * @param atom
	 * @return
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#contains(fr.lirmm.graphik.graal.api.core.Atom)
	 */
	public boolean contains(Atom atom)  {
		return atomset.contains(atom);
	}

	/**
	 * @param atom
	 * @return
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#match(fr.lirmm.graphik.graal.api.core.Atom)
	 */
	public CloseableIteratorWithoutException<Atom> match(Atom atom)  {
		return atomset.match(atom);
	}

	/**
	 * @param predicate
	 * @return
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#atomsByPredicate(fr.lirmm.graphik.graal.api.core.Predicate)
	 */
	public CloseableIteratorWithoutException<Atom> atomsByPredicate(Predicate predicate)  {
		return atomset.atomsByPredicate(predicate);
	}

	/**
	 * @param p
	 * @param position
	 * @return
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#termsByPredicatePosition(fr.lirmm.graphik.graal.api.core.Predicate,
	 *      int)
	 */
	public CloseableIteratorWithoutException<Term> termsByPredicatePosition(Predicate p, int position)  {
		return atomset.termsByPredicatePosition(p, position);
	}

	/**
	 * @return
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#getPredicates()
	 */
	public Set<Predicate> getPredicates()  {
		return atomset.getPredicates();
	}

	/**
	 * @return
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#predicatesIterator()
	 */
	public CloseableIteratorWithoutException<Predicate> predicatesIterator()  {
		return atomset.predicatesIterator();
	}

	/**
	 * @return
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#getTerms()
	 */
	public Set<Term> getTerms()  {
		return atomset.getTerms();
	}

	/**
	 * @return
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#termsIterator()
	 */
	public CloseableIteratorWithoutException<Term> termsIterator()  {
		return atomset.termsIterator();
	}

	/**
	 * @param type
	 * @return
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#getTerms(fr.lirmm.graphik.graal.api.core.Term.Type)
	 */
	public Set<Term> getTerms(Type type)  {
		return atomset.getTerms(type);
	}

	/**
	 * @param type
	 * @return
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#termsIterator(fr.lirmm.graphik.graal.api.core.Term.Type)
	 */
	public CloseableIteratorWithoutException<Term> termsIterator(Type type)  {
		return atomset.termsIterator(type);
	}

	/**
	 * @param atomset
	 * @return
	 * @
	 * @deprecated
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#isSubSetOf(fr.lirmm.graphik.graal.api.core.AtomSet)
	 */
	public boolean isSubSetOf(AtomSet atomset)  {
		return this.atomset.isSubSetOf(atomset);
	}

	/**
	 * @return
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#isEmpty()
	 */
	public boolean isEmpty()  {
		return atomset.isEmpty();
	}

	/**
	 * @param atom
	 * @return
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#add(fr.lirmm.graphik.graal.api.core.Atom)
	 */
	
	public boolean add(Atom atom)  {
		boolean val = atomset.add(atom);
		if(val) {
		if (atom.getPredicate().getArity() == 2) {
			Node n0 = graph.getNode(atom.getTerm(0).getIdentifier().toString());
			if (n0 == null) {
				n0 = graph.addNode(atom.getTerm(0).getIdentifier().toString());
				n0.addAttribute("ui.label", atom.getTerm(0).getIdentifier().toString());
			}
			Node n1 = graph.getNode(atom.getTerm(1).getIdentifier().toString());
			if (n1 == null) {
				n1 = graph.addNode(atom.getTerm(1).getIdentifier().toString());
				n1.addAttribute("ui.label", atom.getTerm(1).getIdentifier().toString());
			}
			Edge addEdge = graph.addEdge(Integer.toString(++edge), n0, n1, true);
			addEdge.addAttribute("ui.class", "new");
			addEdge.addAttribute("ui.label", atom.getPredicate().getIdentifier().toString());
		} else if (atom.getPredicate().getArity() == 1) {
			/*Node n0 = graph.getNode(atom.getTerm(0).getIdentifier().toString());
			if (n0 == null) {
				n0 = graph.addNode(atom.getTerm(0).getIdentifier().toString());
				n0.addAttribute("ui.label", atom.getTerm(0).getIdentifier().toString());
			}
			Node c  = graph.addNode(Integer.toString(++node));
			c.addAttribute("ui.label", atom.getPredicate().getIdentifier().toString());
			c.addAttribute("ui.class", "concept");


			Edge addEdge = graph.addEdge(Integer.toString(++edge), n0, c, true);
			addEdge.addAttribute("ui.label", atom.getPredicate().getIdentifier().toString());*/
		}
		}
		/*
		 * Sprite s = sman.addSprite(n0); s.attachToNode(n0); Sprite s1 =
		 * sman.addSprite(n1); s1.attachToNode(n1);
		 */
		return val;
		
	}

	/**
	 * @param atoms
	 * @return
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#addAll(fr.lirmm.graphik.util.stream.CloseableIterator)
	 */
	public boolean addAll(CloseableIterator<? extends Atom> atoms)  {
	
		try {
			while (atoms.hasNext()) {
				this.add(atoms.next());
			}
			atoms.close();
		} catch (Exception e) {

		}
		return true;
	}

	/**
	 * @param atoms
	 * @return
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#addAll(fr.lirmm.graphik.graal.api.core.AtomSet)
	 */
	public boolean addAll(AtomSet atoms)  {
		
		try {
			CloseableIterator<Atom> it = atoms.iterator();
			while (it.hasNext()) {
				this.add(it.next());
			}
			it.close();

		} catch (Exception e) {

		}
		return true;
	}

	/**
	 * @param atom
	 * @return
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#remove(fr.lirmm.graphik.graal.api.core.Atom)
	 */
	public boolean remove(Atom atom)  {
		return atomset.remove(atom);
	}

	/**
	 * @param atoms
	 * @return
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#removeAll(fr.lirmm.graphik.util.stream.CloseableIterator)
	 */
	public boolean removeAll(CloseableIterator<? extends Atom> atoms)  {
		return atomset.removeAll(atoms);
	}

	/**
	 * @param atoms
	 * @return
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#removeAll(fr.lirmm.graphik.graal.api.core.AtomSet)
	 */
	public boolean removeAll(AtomSet atoms)  {
		return atomset.removeAll(atoms);
	}

	/**
	 * @
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#clear()
	 */
	public void clear()  {
		atomset.clear();
	}

	/**
	 * @return
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#iterator()
	 */
	public CloseableIteratorWithoutException<Atom> iterator() {
		return atomset.iterator();
	}

	/**
	 * @param p
	 * @return
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#count(fr.lirmm.graphik.graal.api.core.Predicate)
	 */
	public int size(Predicate p) {
		return atomset.size(p);
	}

	/**
	 * @return
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#getDomainSize()
	 */
	public int getDomainSize() {
		return atomset.getDomainSize();
	}

	/**
	 * @return
	 * @see fr.lirmm.graphik.graal.api.core.AtomSet#getFreshSymbolGenerator()
	 */
	public ConstantGenerator getFreshSymbolGenerator() {
		return atomset.getFreshSymbolGenerator();
	}
	
	public void clearNew() {
		for(Edge e : graph.getEachEdge()) {
			e.setAttribute("ui.class", "");
		}
	}
	
	public String toString() {
		return this.atomset.toString();
	}

	public Set<Variable> getVariables() {
		return this.atomset.getVariables();
	}

	public Set<Constant> getConstants() {
		return this.atomset.getConstants();
	}

	public Set<Literal> getLiterals() {
		return this.atomset.getLiterals();
	}

	public CloseableIteratorWithoutException<Variable> variablesIterator() {
		return this.atomset.variablesIterator();
	}

	public CloseableIteratorWithoutException<Constant> constantsIterator() {
		return this.atomset.constantsIterator();
	}

	public CloseableIteratorWithoutException<Literal> literalsIterator() {
		return this.atomset.literalsIterator();
	}

	// /////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// OBJECT OVERRIDE METHODS
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS
	// /////////////////////////////////////////////////////////////////////////
	


}
