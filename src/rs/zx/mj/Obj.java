/*
 *  MicroJava Compiler 0.0.1
 * 
 *  Copyright (C) 2016 - Andrej Budinčević
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package rs.zx.mj;

public class Obj {
	public static final int // object kinds
		Con  = 0,
		Var  = 1,
		Type = 2,
		Meth = 3,
		Prog = 4;
	public int    kind;		// Con, Var, Type, Meth, Prog
	public String name;		// object name
	public Struct type;	 	// object type
	public int    val;    // Con: value
	public int    adr;    // Var, Math: address
	public int    level;  // Var: declaration level
	public int    nPars;  // Meth: number of parameters
	public Obj    locals; // Meth: parameters and local objects
	public Obj    next;		// next local object in this scope

	public Obj(int kind, String name, Struct type) {
		this.kind = kind; this.name = name; this.type = type;
	}
}