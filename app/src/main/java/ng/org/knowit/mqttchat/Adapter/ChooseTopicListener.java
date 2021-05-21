package ng.org.knowit.mqttchat.Adapter;

import java.util.ArrayList;

public class ChooseTopicListener implements  SuggestionAdapter.OnListItemClickListener {

    private String choice;
    private ArrayList<String> choices;

    public ChooseTopicListener(ArrayList<String> _choices) {
        choices = _choices;
    }

    public String getChoice() {
        return choice;
    }

    public void onListItemClick(int position) {

        String selectedTopic = choices.get(position);
        //mMessageInput.getInputEditText().setText(" ");
        choice = selectedTopic;
        //mMessageInput.getInputEditText().setText(selectedReply);
    }
}