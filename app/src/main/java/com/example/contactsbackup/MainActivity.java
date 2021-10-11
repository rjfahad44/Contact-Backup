package com.example.contactsbackup;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.jar.Attributes;

public class MainActivity extends AppCompatActivity {

    Button getContactBtn, contactsBackupBtn;
    TextView totalContactsView;
    RecyclerView recyclerView;

    private String [] PERMISSIONS;
    private Cursor cursor;
    private ArrayList<Data> arrayList = new ArrayList<>();
    private Map<String, String> isMap = new HashMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getContactBtn = findViewById(R.id.getContactBtn);
        contactsBackupBtn = findViewById(R.id.backupContacts);
        recyclerView = findViewById(R.id.recyclerView);
        totalContactsView = findViewById(R.id.totalContactsView);

        PERMISSIONS = new String[]{
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
        };

        if (!isPermission(MainActivity.this, PERMISSIONS)){
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, 103);
        }

        getContactBtn.setOnClickListener(v -> {
            showContacts(cursor);
        });

        contactsBackupBtn.setOnClickListener(v -> {
            if (isCursorEmpty(cursor)){
                Toast.makeText(MainActivity.this, "Contact list is empty!", Toast.LENGTH_SHORT).show();
            }else {
                createPDF();
            }
        });

        getContacts();
    }

    public boolean isCursorEmpty(Cursor cursor){
        return !cursor.moveToFirst() || cursor.getCount() == 0;
    }

    private boolean isPermission(Context context, String...PERMISSIONS){
        if (context!= null && PERMISSIONS!= null){
            for (String permission: PERMISSIONS){
                if (ActivityCompat.checkSelfPermission(context, permission)!= PackageManager.PERMISSION_GRANTED){
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                startActivityForResult(intent, 104);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, 104);
                e.printStackTrace();
            }
        }

        if (requestCode == 103){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
//                Toast.makeText(this, "Contacts Read Permission Granted", Toast.LENGTH_SHORT).show();
            }else {
//                Toast.makeText(this, "Contacts Read Permission Denied", Toast.LENGTH_SHORT).show();
            }

            if (grantResults[1] == PackageManager.PERMISSION_GRANTED){
//                Toast.makeText(this, "Storage Read Permission Granted", Toast.LENGTH_SHORT).show();
            }else {
//                Toast.makeText(this, "Storage Read Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getContacts(){
        cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null, null, null, null );
        startManagingCursor(cursor);
    }

    private void showContacts(Cursor cursor){

        while (cursor.moveToNext()) {
            String ID = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID));
            String Name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String Number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            isMap.put(Name, Number);
        }

        totalContactsView.setText("Total Number Of Contacts : "+ isMap.size());

        for (Map.Entry<String, String> i: isMap.entrySet()){
            Log.d("TAG", "passed : "+i.getKey()+" "+i.getValue()+" "+isMap.size());
            arrayList.add(new Data(i.getKey(), i.getValue()));
        }

        RecyclerAdapter recyclerAdapter = new RecyclerAdapter(arrayList);
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void createPDF() {

        try {
            String folderPath = Environment.getExternalStorageDirectory().toString();
            File newFolder = new File(folderPath, "/My_Contacts_Backup");
            if (!newFolder.mkdirs()){
                newFolder.mkdirs();
            }

            String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
            String pdfName = "my_Contacts_"+year+".pdf";
            File file = new File(newFolder, pdfName);


            PdfWriter pdfWriter = new PdfWriter(file);
            PdfDocument pdfDocument = new PdfDocument(pdfWriter);
            Document document = new Document(pdfDocument);


            int totalContacts = isMap.size();

            Paragraph TableHead = new Paragraph("MY CONTACTS LIST PDF").setBold().setFontSize(24).setTextAlignment(TextAlignment.CENTER);
            Paragraph TotalContacts = new Paragraph("Total Number of Contacts : "+ totalContacts).setFontSize(12).setTextAlignment(TextAlignment.CENTER);

            //Current Date And Time Function//
            String currentDate = new SimpleDateFormat("EEE, d MMM, yyyy", Locale.getDefault()).format(new Date());
            String currentTime = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(new Date());

            Paragraph DateTime = new Paragraph("Date : "+ currentDate + "   " + "Time : " + currentTime).setFontSize(12).setTextAlignment(TextAlignment.CENTER);

            //Table Column No.//
            float [] width = {70f,150f, 150f};

            //Table Create//
            Table table = new Table(width);

            //Table Position Center of the page//
            table.setHorizontalAlignment(HorizontalAlignment.CENTER);


            //Add Table Head//
            table.addCell(new Cell().add(new Paragraph("  No."))).setBold();
            table.addCell(new Cell().add(new Paragraph("  Name"))).setBold();
            table.addCell(new Cell().add(new Paragraph("  Phone Number"))).setBold();


            //Add Table Data//
            int counter = 1;
            for (Map.Entry<String, String> i: isMap.entrySet()){

                arrayList.add(new Data(i.getKey(), i.getValue()));

                table.addCell(new Cell().add(new Paragraph("  "+counter+".")));
                table.addCell(new Cell().add(new Paragraph("  "+i.getKey())));
                table.addCell(new Cell().add(new Paragraph("  "+i.getValue())));
            counter++;
        }

//            document.add(image);
            document.add(TableHead);
            document.add(DateTime);
            document.add(TotalContacts);
            document.add(table);

            document.close();


            Toast.makeText(this, "PDF Created Successfully : " + file, Toast.LENGTH_LONG).show();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
//            Log.d("E", "exception : "+ e.getMessage());
        }
    }
}