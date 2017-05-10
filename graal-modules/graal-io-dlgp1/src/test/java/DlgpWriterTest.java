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
 import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.Term;
import fr.lirmm.graphik.graal.core.DefaultAtom;
import fr.lirmm.graphik.graal.core.term.DefaultTermFactory;
import fr.lirmm.graphik.graal.io.dlp.Dlgp1Writer;


/**
 * @author Clément Sipieter (INRIA) <clement@6pi.fr>
 *
 */
public class DlgpWriterTest {
	
	private static Predicate predicat = new Predicate("p", 1);
	private static Term cst = DefaultTermFactory.instance().createConstant("A");
	
	@Test
	public void writeConstant() throws IOException {
		Term a = DefaultTermFactory.instance().createConstant("A");
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Dlgp1Writer writer = new Dlgp1Writer(os);
		
		writer.write(new DefaultAtom(predicat, a));
		writer.flush();

		String s = new String(os.toByteArray(),"UTF-8");
		writer.close();
		
		char c = s.charAt(s.indexOf("(") + 1);
		Assert.assertTrue("Constant label does not begin with lower case.", Character.isLowerCase(c) || c == '<');
	}
	
	@Test
	public void writeVariable() throws IOException {
		Term x = DefaultTermFactory.instance().createVariable("x");
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Dlgp1Writer writer = new Dlgp1Writer(os);
		
		writer.write(new DefaultAtom(predicat, x));
		writer.flush();

		String s = new String(os.toByteArray(),"UTF-8");
		writer.close();
		Assert.assertTrue("Variable label does not begin with upper case.", Character.isUpperCase(s.charAt(s.indexOf("(") + 1)));
	}
	
	@Test
	public void writePredicate() throws IOException {
		Predicate p = new Predicate("P", 1);
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Dlgp1Writer writer = new Dlgp1Writer(os);
		
		writer.write(new DefaultAtom(p, cst));
		writer.flush();

		String s = new String(os.toByteArray(),"UTF-8");
		writer.close();
		
		Character c = s.charAt(0);
		Assert.assertTrue("Predicate label does not begin with lower case or double quote.", Character.isLowerCase(c) || c == '"');
	}

}
