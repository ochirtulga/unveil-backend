-- Unveil Sample Data
-- This file automatically loads when the application starts

-- Tech Support Scams
INSERT INTO bad_actors (name, email, phone, company, description, actions, reported_by, created_at) VALUES
('John Microsoft', 'support@fake-microsoft.com', '+1-800-123-4567', 'Fake Microsoft Support', 'Claims to be from Microsoft, asking for remote desktop access to fix computer virus.', 'Tech Support', 'FBI Scam Alert', '2024-01-15 10:30:00'),
('Sarah Windows', 'help@windows-security.net', '+1-888-999-7777', 'Windows Security Team', 'Cold calls claiming computer is infected, requests credit card for security software.', 'Tech Support', 'BBB Report', '2024-01-20 14:22:00'),
('Mike Apple', 'security@apple-support.org', '+1-877-555-0199', 'Apple Security Division', 'Phishing emails claiming iCloud account compromised, requests password reset.', 'Tech Support', 'User Report', '2024-02-01 09:15:00'),

-- Romance Scams
('David Johnson', 'david.johnson.love@gmail.com', '+234-802-123-4567', NULL, 'Claims to be US soldier overseas, builds romantic relationship then asks for money for emergency.', 'Romance', 'Military Investigation', '2024-01-10 16:45:00'),
('Maria Santos', 'maria.santos.model@hotmail.com', '+63-915-555-0123', 'Fashion Model Agency', 'Uses stolen photos of models, claims to be traveling fashion model needing money for travel.', 'Romance', 'Interpol Alert', '2024-01-25 11:30:00'),
('Robert Williams', 'robert.w.doctor@yahoo.com', '+44-20-7946-0958', 'London Medical Center', 'Claims to be British doctor working with WHO, asks for money for medical supplies.', 'Romance', 'User Report', '2024-02-05 13:20:00'),

-- Investment Scams
('Jennifer Chen', 'invest@crypto-millions.com', '+1-646-555-0188', 'Crypto Millions LLC', 'Promises guaranteed returns on cryptocurrency investments, sophisticated fake trading platform.', 'Investment', 'SEC Investigation', '2024-01-08 08:00:00'),
('Mark Thompson', 'mark@forex-elite.net', '+1-305-555-0177', 'Forex Elite Trading', 'Claims to be professional forex trader, offers to manage investments with 500% returns.', 'Investment', 'CFTC Alert', '2024-01-18 15:10:00'),
('Lisa Rodriguez', 'lisa@goldmine-investments.com', '+1-713-555-0166', 'Goldmine Investments', 'Fake gold mining investment opportunity, professional website and documentation.', 'Investment', 'FTC Warning', '2024-02-03 12:45:00'),

-- Phone/SMS Scams
('Alex Kumar', NULL, '+1-202-555-0155', 'IRS Tax Division', 'Robocalls claiming tax debt, threatens arrest if immediate payment not made via gift cards.', 'Tax/IRS', 'IRS Scam Alert', '2024-01-12 10:15:00'),
('Rachel Green', NULL, '+1-512-555-0144', 'Social Security Administration', 'Claims Social Security number suspended due to suspicious activity, requests verification.', 'Government', 'SSA Warning', '2024-01-22 14:30:00'),
('Carlos Martinez', NULL, '+1-310-555-0133', 'Auto Warranty Center', 'Extended car warranty robocalls, high-pressure tactics to purchase overpriced warranties.', 'Auto Warranty', 'Consumer Report', '2024-02-07 16:20:00'),

-- Email/Phishing Scams
('Prince William Okafor', 'prince.okafor@nigeria-treasury.gov', NULL, 'Nigerian Treasury', 'Classic Nigerian prince scam, offers millions in exchange for bank account information.', 'Nigerian Prince', 'Classic Scam', '2024-01-05 12:00:00'),
('Amazon Security', 'security-alert@amazon-verification.com', NULL, 'Amazon', 'Fake Amazon security alerts claiming account compromised, harvests login credentials.', 'Phishing', 'Amazon Report', '2024-01-14 09:30:00'),
('PayPal Support', 'support@paypal-security.net', NULL, 'PayPal', 'Fake PayPal emails claiming account limitation, redirects to fake login page.', 'Phishing', 'PayPal Alert', '2024-01-28 11:45:00'),

-- Job/Employment Scams
('Sarah Employment', 'careers@work-from-home-now.com', '+1-888-555-0122', 'Work From Home Solutions', 'Fake work-from-home opportunities, requires upfront payment for training materials.', 'Employment', 'Job Board Report', '2024-01-16 13:15:00'),
('Tom Recruiter', 'tom.recruiter@quick-cash-jobs.net', '+1-844-555-0111', 'Quick Cash Jobs', 'Promises easy money for simple tasks, requires personal information and bank details.', 'Employment', 'User Report', '2024-02-02 10:45:00'),
('Global Opportunities', 'hr@global-remote-work.com', '+1-866-555-0100', 'Global Remote Work Ltd', 'Fake international job opportunities, requests fees for visa processing.', 'Employment', 'BBB Warning', '2024-02-06 15:30:00'),

-- Online Shopping/Marketplace Scams
('Discount Electronics', 'sales@discount-electronics-outlet.com', '+1-877-555-0199', 'Electronics Outlet', 'Fake electronics store with incredibly low prices, takes payment but never ships products.', 'Shopping', 'Consumer Protection', '2024-01-11 14:20:00'),
('Luxury Watches', 'info@luxury-watches-direct.net', '+1-855-555-0188', 'Luxury Watches Direct', 'Fake luxury watch retailer, sells counterfeit products or takes money without shipping.', 'Shopping', 'Trademark Violation', '2024-01-26 12:10:00'),
('Pet Supplies Plus', 'orders@pet-supplies-mega.com', '+1-833-555-0177', 'Pet Supplies Mega', 'Fake pet supply store, particularly targets pet owners looking for rare or expensive items.', 'Shopping', 'Pet Owner Alert', '2024-02-04 09:55:00'),

-- Charity/Donation Scams
('Disaster Relief Fund', 'donations@emergency-relief-now.org', '+1-800-555-0166', 'Emergency Relief Foundation', 'Fake charity claiming to help disaster victims, sophisticated website and testimonials.', 'Charity', 'Charity Navigator', '2024-01-13 16:40:00'),
('Children Help International', 'donate@children-help-international.net', '+1-888-555-0155', 'Children Help International', 'Fake children charity, uses emotional appeals and fake photos to solicit donations.', 'Charity', 'Charity Watchdog', '2024-01-29 11:25:00'),

-- Health/Medical Scams
('Dr. Natural Remedies', 'doctor@miracle-cures.com', '+1-800-555-0144', 'Miracle Cures Institute', 'Sells fake miracle cures and supplements, claims to cure serious diseases.', 'Health', 'FDA Warning', '2024-01-17 13:50:00'),
('Weight Loss Solutions', 'info@lose-weight-fast.net', '+1-877-555-0133', 'Fast Weight Loss Solutions', 'Fake weight loss products with impossible claims, auto-billing subscription trap.', 'Health', 'FTC Action', '2024-02-08 10:20:00'),

-- Travel/Vacation Scams
('Dream Vacations', 'booking@dream-vacations-now.com', '+1-855-555-0122', 'Dream Vacations', 'Fake vacation package deals, takes payment for trips that do not exist.', 'Travel', 'Travel Association', '2024-01-19 15:35:00'),
('Timeshare Resale', 'sales@timeshare-resale-experts.net', '+1-844-555-0111', 'Timeshare Resale Experts', 'Fake timeshare resale company charges upfront fees but never sells timeshares.', 'Travel', 'Real Estate Commission', '2024-02-01 12:15:00');

-- Add some entries with partial information (common in real scenarios)
INSERT INTO bad_actors (name, email, phone, company, description, actions, created_at) VALUES
('Unknown Caller', NULL, '+1-800-123-SCAM', NULL, 'Robocall claiming car warranty expiration', 'Robocall', '2024-02-09 14:30:00'),
(NULL, 'winner@fake-lottery.com', NULL, 'International Lottery Commission', 'You have won lottery email scam', 'Lottery', '2024-02-10 09:15:00'),
('Tech Support', 'virus-alert@computer-fix.net', '+1-888-VIRUS-1', 'Computer Fix Solutions', 'Pop-up virus warning leading to fake tech support', 'Tech Support', '2024-02-11 16:45:00');