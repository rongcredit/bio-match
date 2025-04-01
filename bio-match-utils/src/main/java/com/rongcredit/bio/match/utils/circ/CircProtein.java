package com.rongcredit.bio.match.utils.circ;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CircProtein {

	private String protein;
	private List<Integer> boundarys;
}
