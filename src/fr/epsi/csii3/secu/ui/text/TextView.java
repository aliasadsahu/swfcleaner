package fr.epsi.csii3.secu.ui.text;

import java.awt.GridLayout;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import fr.epsi.csii3.secu.ui.generic.AbstractView;

@SuppressWarnings("serial")
public class TextView extends AbstractView {
	private JTextArea textArea;
	
	public TextView() {
		this.model = new TextModel();
		this.model.addModelChangedListener(this);
		setLayout(new GridLayout(1,1));
		this.textArea = new JTextArea();
		JScrollPane scrollPane = new JScrollPane(this.textArea);
		add(scrollPane);
	}

	public JTextArea getTextArea() {
		return this.textArea;
	}

	@Override
	public void modelHasChanged() {
		this.textArea.setText(((TextModel)model).getText());
	}
}
