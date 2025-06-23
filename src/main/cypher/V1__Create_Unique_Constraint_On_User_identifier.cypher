// as test user
CREATE CONSTRAINT FOR (u:User) REQUIRE u.name IS UNIQUE;
