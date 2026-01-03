import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { Layout } from './components/Layout';
import { Home } from './pages/Home';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Profile } from './pages/Profile';
import { SellerProfile } from './pages/SellerProfile';
import { Listings } from './pages/Listings';
import { ListingForm } from './pages/ListingForm';
import { ListingDetail } from './pages/ListingDetail';
import { ListingPhotos } from './pages/ListingPhotos';
import { Search } from './pages/Search';
import { Favourites } from './pages/Favourites';
import { ReviewForm } from './pages/ReviewForm';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Layout>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/profile" element={<Profile />} />
            <Route path="/seller/profile" element={<SellerProfile />} />
            <Route path="/listings" element={<Listings />} />
            <Route path="/listings/create" element={<ListingForm />} />
            <Route path="/listings/:id" element={<ListingDetail />} />
            <Route path="/listings/:id/edit" element={<ListingForm />} />
            <Route path="/listings/:id/photos" element={<ListingPhotos />} />
            <Route path="/listings/:id/review" element={<ReviewForm />} />
            <Route path="/search" element={<Search />} />
            <Route path="/favourites" element={<Favourites />} />
          </Routes>
        </Layout>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;

