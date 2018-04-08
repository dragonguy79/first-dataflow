package com.example.gym;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by engkhangloh on 31/03/2018.
 */
public class CreateFitnessData {

    public static void main(String[] args) {
        //ID1,Tommy,Hanks,1960-09-21,M,186,95,2018-03-01,7.3,2501,980,26,90
        List<Member> members = new ArrayList<>();

//        members.add(new Member("ID1", "Albert", "Noth", "1965-09-23", "M", 185, 88.9));
//        members.add(new Member("ID2", "Julia", "Strassen", "1988-03-01", "F", 168, 58.5));
//        members.add(new Member("ID3", "Sam", "Lokkel", "1985-11-25", "M", 180, 78.2));
//        members.add(new Member("ID4", "Larry", "Padington", "1975-10-25", "M", 182, 73.1));
//        members.add(new Member("ID5", "Molly", "Brin", "1984-02-21", "F", 172, 59.1));
//        members.add(new Member("ID6", "Sonia", "Cheah", "1994-02-21", "F", 162, 51.2));
//        members.add(new Member("ID7", "James", "Colly", "1988-12-11", "M", 176, 69.1));
//        members.add(new Member("ID8", "Matt", "Dash", "1973-12-11", "M", 178, 77.1));
//        members.add(new Member("ID9", "Sammy", "Lon", "1972-10-11", "M", 171, 79.1));
        members.add(new Member("ID10", "Manny", "Lors", "1992-01-21", "F", 174, 67.1));

        for (Member member : members) {
            LocalDate date = LocalDate.now().minusYears(1);
            for (int i=0; i<365; i++) {
                StringBuffer sb = new StringBuffer();
                sb.append(member.id + ",");
                sb.append(member.firstName + ",");
                sb.append(member.lastName + ",");
                sb.append(member.birthday + ",");
                sb.append(member.gender + ",");
                sb.append(member.height + ",");
                sb.append(member.weight + ",");
                sb.append(date.toString() + ",");
                Double leftLimit = 5d;
                Double rightLimit = 9d;
                Double hourSleep = leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
                sb.append(round(hourSleep, 2) + ",");
                sb.append((new Random().nextInt(2000) + 1000) + ",");
                sb.append((new Random().nextInt(1000) + 800) + ",");
                sb.append(round(member.getBmi(), 2) + ",");
                sb.append((new Random().nextInt(40) + 60));
                System.out.println(sb.toString());
                date = date.plusDays(1);
            }
        }
    }

    private static double round (double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    public static class Member {
        private String id;
        private String firstName;
        private String lastName;
        private String birthday;
        private String gender;
        private int height;
        private Double weight;

        public Member(String id, String firstName, String lastName, String birthday, String gender, int height, Double weight) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.birthday = birthday;
            this.gender = gender;
            this.height = height;
            this.weight = weight;
        }

        public String getId() {
            return id;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getBirthday() {
            return birthday;
        }

        public String getGender() {
            return gender;
        }

        public int getHeight() {
            return height;
        }

        public Double getWeight() {
            return weight;
        }

        public Double getBmi() {
            return weight / Math.pow(new Double(height) / 100, 2);
        }
    }
}
