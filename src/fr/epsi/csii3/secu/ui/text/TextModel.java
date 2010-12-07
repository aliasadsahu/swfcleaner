package fr.epsi.csii3.secu.ui.text;

import fr.epsi.csii3.secu.ui.generic.AbstractModel;

public class TextModel extends AbstractModel {
	
	private String text;

	public String getText() {
		return this.text;
	}

	public void setText(String text) {
		this.text = text;
		super.notifyChanges();
	}

}
