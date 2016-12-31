package org.purple.smoke;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

public class Settings extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final Button button1 = (Button) findViewById
	    (R.id.reset_neighbor_fields);

        button1.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    final RadioButton radioButton1 = (RadioButton) findViewById
			(R.id.neighbors_ipv4);
		    final Spinner spinner1 = (Spinner) findViewById
			(R.id.neighbors_transport);
		    final TextView textView1 = (TextView) findViewById
			(R.id.neighbors_ip_address);
		    final TextView textView2 = (TextView) findViewById
			(R.id.neighbors_port);
		    final TextView textView3 = (TextView) findViewById
			(R.id.neighbors_scope_id);

		    radioButton1.setChecked(true);
		    spinner1.setSelection(0);
		    textView1.setText("");
		    textView2.setText("4710");
		    textView3.setText("");
		    textView1.requestFocus();
		}
	    });

        final Button button2 = (Button) findViewById(R.id.set_password);

        button2.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    final TextView textView1 = (TextView) findViewById
			(R.id.password1);
		    final TextView textView2 = (TextView) findViewById
			(R.id.password2);

		    if(!textView1.getText().toString().
		       equals(textView2.getText().toString()) ||
		       textView1.getText().length() < 16) {
			AlertDialog alertDialog = new AlertDialog.Builder
			    (Settings.this).create();

			if(textView1.getText().length() < 16)
			    alertDialog.setMessage("Each password must contain at least sixteen characters.");
			else
			    alertDialog.setMessage("The provided passwords are not identical.");

			alertDialog.setTitle("Error");
			alertDialog.setButton
			    (AlertDialog.BUTTON_NEUTRAL, "OK",
			     new DialogInterface.OnClickListener() {
				 public void onClick(DialogInterface dialog,
						     int which) {
				     dialog.dismiss();
				 }
			     });

			alertDialog.show();
		    }
		}
	    });

        Button button3 = (Button) findViewById(R.id.add_neighbor);

        button3.setEnabled(false);
        button3 = (Button) findViewById(R.id.delete_neighbor);
        button3.setEnabled(false);
        button3 = (Button) findViewById(R.id.refresh_neighbors);
        button3.setEnabled(false);

        RadioButton radioButton1 = (RadioButton) findViewById
	    (R.id.neighbors_ipv4);

        radioButton1.setEnabled(false);
        radioButton1 = (RadioButton) findViewById(R.id.neighbors_ipv6);
        radioButton1.setEnabled(false);

        Spinner spinner1 = (Spinner) findViewById(R.id.neighbors_transport);

        spinner1.setEnabled(false);

        String array[] = new String[] {
                "TCP", "UDP"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>
				(this, android.R.layout.simple_spinner_item, array);

        spinner1.setAdapter(adapter);

        final RadioGroup radioGroup1 = (RadioGroup) findViewById
	    (R.id.neighbors_ipv_radio_group);

        radioGroup1.setOnCheckedChangeListener
	    (new RadioGroup.OnCheckedChangeListener() {
		    public void onCheckedChanged(RadioGroup group,
						 int checkedId) {
			final TextView textView1 = (TextView) findViewById
			    (R.id.neighbors_scope_id);

			if(checkedId == R.id.neighbors_ipv4) {
			    textView1.setText("");
			    textView1.setVisibility(View.GONE);
			} else
			    textView1.setVisibility(View.VISIBLE);
		    }
		});

        TextView textView1 = (TextView) findViewById(R.id.neighbors_scope_id);

        textView1.setEnabled(false);
        textView1.setVisibility(View.GONE);
        textView1 = (TextView) findViewById(R.id.neighbors_port);
        textView1.setEnabled(false);
        textView1.setText("4710");
        textView1 = (TextView) findViewById(R.id.neighbors_ip_address);
        textView1.setEnabled(false);
        textView1 = (TextView) findViewById(R.id.password1);
	textView1.requestFocus();
        textView1.setText("");
        textView1 = (TextView) findViewById(R.id.password2);
        textView1.setText("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == R.id.action_authenticate) {
            final Intent intent = new Intent(Settings.this, Authenticate.class);

            startActivity(intent);
            return true;
        } else if(id == R.id.action_chat) {
            final Intent intent = new Intent(Settings.this, Chat.class);

            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
