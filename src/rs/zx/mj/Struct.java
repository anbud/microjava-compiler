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

public class Struct {
	public static final int // structure kinds
		None  = 0,
		Int   = 1,
		Char  = 2,
		Arr   = 3,
		Class = 4;
	public int    kind;		  // None, Int, Char, Arr, Class
	public Struct elemType; // Arr: element type
	public int    nFields;  // Class: number of fields
	public Obj    fields;   // Class: fields

	public Struct(int kind) {
		this.kind = kind;
	}

	public Struct(int kind, Struct elemType) {
		this.kind = kind; this.elemType = elemType;
	}

	// Checks if this is a reference type
	public boolean isRefType() {
		return kind == Class || kind == Arr;
	}

	// Checks if two types are equal
	public boolean equals(Struct other) {
		if (kind == Arr)
			return other.kind == Arr && other.elemType == elemType;
		else
			return other == this;
	}

	// Checks if two types are compatible (e.g. in a comparison)
	public boolean compatibleWith(Struct other) {
		return this.equals(other)
			||	this == Tab.nullType && other.isRefType()
			||	other == Tab.nullType && this.isRefType();
	}

	// Checks if an object with type "this" can be assigned to an object with type "dest"
	public boolean assignableTo(Struct dest) {
		return this.equals(dest)
			||	this == Tab.nullType && dest.isRefType()
			||  this.kind == Arr && dest.kind == Arr && dest.elemType == Tab.noType;
	}

}