// as test user
CREATE CONSTRAINT FOR (u:User) REQUIRE u.identifier IS UNIQUE;
